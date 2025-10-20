(ns ezand.retro2mqtt.udp
  (:require [clojure.string :as str])
  (:import (java.net DatagramPacket DatagramSocket InetAddress SocketTimeoutException)))

(def ^:const default-udp-timeout (long 10000))

(defn send-udp-sync
  [host port timeout-ms message]
  (let [^InetAddress addr (InetAddress/getByName host)
        ^bytes send-buf (.getBytes message "UTF-8")
        send-packet (DatagramPacket. send-buf (int (count send-buf)) addr (int port))
        ^bytes recv-buf (byte-array 4096)
        recv-packet (DatagramPacket. recv-buf (int 4096))
        timeout (or timeout-ms default-udp-timeout)]
    (with-open [socket (DatagramSocket.)]
      (try
        (.setSoTimeout socket timeout)
        (.send socket send-packet)
        (.receive socket recv-packet)
        (-> (String. (.getData recv-packet) 0 (.getLength recv-packet) "UTF-8")
            (str/trim))
        (catch SocketTimeoutException _
          (throw (ex-info "UDP receive timeout" {:host host :port port :timeout-ms timeout})))
        (catch Exception e
          (throw e))
        (finally
          (when socket
            (.close socket)))))))
