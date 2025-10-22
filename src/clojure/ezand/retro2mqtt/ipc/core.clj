(ns ezand.retro2mqtt.ipc.core
  (:require [clojure.core.async :as async :refer [<! >! go-loop]]
            [ezand.retro2mqtt.logger :as log]
            [ezand.retro2mqtt.utils :as util]
            [superstring.core :as str])
  (:import (java.io BufferedReader InputStream InputStreamReader OutputStream PrintWriter)
           (java.net StandardProtocolFamily UnixDomainSocketAddress)
           (java.nio.channels Channels ServerSocketChannel SocketChannel)
           (java.nio.charset StandardCharsets)
           (java.nio.file Paths)
           (org.scalasbt.ipcsocket Win32NamedPipeServerSocket Win32NamedPipeSocket)))

(def ^:private logger (log/create-logger! *ns*))

;;;;;;;;;;;
;; Utils ;;
;;;;;;;;;;;
(defn- pipe->input-stream
  "Get InputStream from either Win32NamedPipeSocket or SocketChannel"
  [pipe-connection]
  (if (instance? SocketChannel pipe-connection)
    (Channels/newInputStream ^SocketChannel pipe-connection)
    (.getInputStream pipe-connection)))

(defn- pipe->output-stream
  "Get OutputStream from either Win32NamedPipeSocket or SocketChannel"
  [pipe-connection]
  (if (instance? SocketChannel pipe-connection)
    (Channels/newOutputStream ^SocketChannel pipe-connection)
    (.getOutputStream pipe-connection)))

;;;;;;;;;;;;
;; Server ;;
;;;;;;;;;;;;
(defn create-pipe-server
  "Creates a named pipe server to receive messages from HyperSpin/EDS"
  [pipe-path]
  (util/with-suppressed-errors logger (str "Unable to create pipe server: " pipe-path)
    (case util/current-os
      :windows (Win32NamedPipeServerSocket. pipe-path)
      (let [path (Paths/get pipe-path (into-array String []))
            addr (UnixDomainSocketAddress/of path)
            socket-file (.toFile path)]
        ;; Delete stale socket file if it exists
        (when (.exists socket-file)
          (.delete socket-file))
        (doto (ServerSocketChannel/open StandardProtocolFamily/UNIX)
          (.configureBlocking true)
          (.bind addr))))))

;;;;;;;;;;;;
;; Client ;;
;;;;;;;;;;;;
(defn create-pipe-client
  "Creates a named pipe client to send messages to HyperSpin/EDS"
  [pipe-path]
  (util/with-suppressed-errors logger (str "Error connecting to pipe: " pipe-path)
    (case util/current-os
      :windows (Win32NamedPipeSocket. pipe-path)
      (let [path (Paths/get pipe-path (into-array String []))
            addr (UnixDomainSocketAddress/of path)]
        (doto (SocketChannel/open StandardProtocolFamily/UNIX)
          (.configureBlocking true)
          (.connect addr))))))

;;;;;;;;;;;;;;;;;;;;
;; Read from pipe ;;
;;;;;;;;;;;;;;;;;;;;
(defn read-pipe-message
  "Reads a message from the pipe input stream"
  [^InputStream input-stream]
  (let [reader (BufferedReader. (InputStreamReader. input-stream StandardCharsets/UTF_8))]
    (util/with-suppressed-errors logger "Error reading from pipe"
      (when-let [line (.readLine reader)]
        (when-not (str/blank? line)
          line)))))

;;;;;;;;;;;;;;;;;;;
;; Write to pipe ;;
;;;;;;;;;;;;;;;;;;;
(defn send-pipe-message
  "Sends a message through the named pipe"
  [socket message]
  (util/with-suppressed-errors logger "Error sending message"
    (let [^OutputStream output-stream (pipe->output-stream socket)
          writer (PrintWriter. output-stream true)]
      (.println writer message)
      (.flush writer)
      true)))

;;;;;;;;;;;;;;;;;;;
;; Pipe listener ;;
;;;;;;;;;;;;;;;;;;;
(defn start-pipe-listener
  "Starts listening on the named pipe for events"
  [pipe-path event-chan parse-fn]
  (go-loop []
    (when-let [server (create-pipe-server pipe-path)]
      (try
        (log/info logger "Waiting for pipe connection" {:pipe-path pipe-path})
        (let [client (.accept server)
              input-stream (pipe->input-stream client)]
          (log/info logger "Connected to pipe!" {:pipe-path pipe-path})

          ;; Keep reading messages until connection closes
          (loop []
            (when-let [message (read-pipe-message input-stream)]
              (log/trace logger "Received pipe message" {:pipe-path pipe-path :message message})
              (let [parsed (parse-fn message)]
                (>! event-chan parsed))
              (recur)))

          (.close client))
        (catch Throwable e
          (log/error logger "Pipe listener error" {:pipe-path pipe-path :exception e}))
        (finally
          ;; Clean up Unix socket file on shutdown
          (when (instance? ServerSocketChannel server)
            (when-let [addr (.getLocalAddress ^ServerSocketChannel server)]
              (when (instance? UnixDomainSocketAddress addr)
                (util/with-suppressed-errors logger (str "Error deleting socket file: " pipe-path)
                  (.delete (.toFile (.getPath ^UnixDomainSocketAddress addr)))))))
          (.close server)))

      ;; Reconnect after delay
      (<! (async/timeout 2000))
      (recur))))
