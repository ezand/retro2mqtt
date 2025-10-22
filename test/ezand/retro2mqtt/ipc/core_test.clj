(ns ezand.retro2mqtt.ipc.core-test
  (:require [clojure.core.async :as async :refer [<!! chan timeout]]
            [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.ipc.core :as ipc]
            [ezand.retro2mqtt.utils :as util])
  (:import (java.io ByteArrayInputStream File)))

(deftest read-pipe-message-test
  (testing "read-pipe-message handles valid input"
    (let [input-stream (ByteArrayInputStream. (.getBytes "test message\n" "UTF-8"))
          result (ipc/read-pipe-message input-stream)]
      (is (= "test message" result))))

  (testing "read-pipe-message returns nil for blank lines"
    (let [input-stream (ByteArrayInputStream. (.getBytes "   \n" "UTF-8"))
          result (ipc/read-pipe-message input-stream)]
      (is (nil? result))))

  (testing "read-pipe-message returns nil for empty stream"
    (let [input-stream (ByteArrayInputStream. (.getBytes "" "UTF-8"))
          result (ipc/read-pipe-message input-stream)]
      (is (nil? result)))))

(deftest unix-socket-lifecycle-test
  (testing "Unix socket server lifecycle"
    (when (= util/current-os :unix-style)
      (let [socket-path "/tmp/test-retro2mqtt-socket"
            socket-file (File. socket-path)]
        ;; Cleanup before test
        (when (.exists socket-file)
          (.delete socket-file))

        (testing "creates socket file"
          (when-let [server (ipc/create-pipe-server socket-path)]
            (try
              (is (.exists socket-file) "Socket file should be created")
              (finally
                (.close server)
                (.delete socket-file)))))

        (testing "deletes stale socket file on startup"
          ;; Create stale socket file
          (.createNewFile socket-file)
          (is (.exists socket-file) "Stale socket file exists")

          (when-let [server (ipc/create-pipe-server socket-path)]
            (try
              (is (.exists socket-file) "Socket file should exist after server creation")
              (finally
                (.close server)
                (.delete socket-file)))))))))

(deftest pipe-communication-test
  (testing "Full pipe communication cycle"
    (let [socket-path (if (= util/current-os :windows)
                        "\\\\.\\pipe\\test-retro2mqtt"
                        "/tmp/test-retro2mqtt-comm")
          event-chan (chan 10)
          test-message "test-event"
          parse-fn identity]

      ;; Start listener in background
      (ipc/start-pipe-listener socket-path event-chan parse-fn)

      ;; Give server time to start
      (<!! (timeout 500))

      (testing "client can connect and send message"
        (when-let [client (ipc/create-pipe-client socket-path)]
          (try
            (is (true? (ipc/send-pipe-message client test-message)))

            ;; Wait for message to be received
            (let [received (async/alt!!
                             event-chan ([msg] msg)
                             (timeout 2000) :timeout)]
              (is (= test-message received) "Should receive the sent message"))
            (finally
              (.close client)))))

      ;; Cleanup
      (when (= util/current-os :unix-style)
        (let [socket-file (File. socket-path)]
          (when (.exists socket-file)
            (.delete socket-file)))))))
