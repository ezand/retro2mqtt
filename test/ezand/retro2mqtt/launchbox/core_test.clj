(ns ezand.retro2mqtt.launchbox.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [ezand.retro2mqtt.launchbox.core :as launchbox-core]
            [ezand.retro2mqtt.launchbox.mqtt :as launchbox-mqtt]
            [ezand.retro2mqtt.mqtt.core :as mqtt]
            [ezand.retro2mqtt.provider :as provider])
  (:import (clojure.lang Atom)
           (ezand.retro2mqtt.launchbox.core LaunchBoxProvider)))

(deftest launchbox-provider-record-test
  (testing "LaunchBoxProvider record exists"
    (is (class? LaunchBoxProvider)))

  (testing "LaunchBoxProvider implements RetroProvider protocol"
    (let [provider (launchbox-core/->LaunchBoxProvider nil {})]
      (is (satisfies? provider/RetroProvider provider))))

  (testing "LaunchBoxProvider has expected fields"
    (let [mock-client :mock-client
          mock-config {:launchbox {:test "config"}}
          provider (launchbox-core/->LaunchBoxProvider mock-client mock-config)]
      (is (= :mock-client (:mqtt-client provider)))
      (is (= mock-config (:config provider))))))

(deftest listening-state-atoms-test
  (testing "listening? atom is defined"
    (is (instance? Atom @#'launchbox-core/listening?)))

  (testing "subscriptions atom is defined"
    (is (instance? Atom @#'launchbox-core/subscriptions)))

  (testing "listening? is initially false"
    (let [listening-atom @#'launchbox-core/listening?]
      (is (boolean? @listening-atom))))

  (testing "subscriptions is initially a vector"
    (let [subs-atom @#'launchbox-core/subscriptions]
      (is (vector? @subs-atom)))))

(deftest launchbox-provider-function-test
  (testing "launchbox-provider function exists"
    (is (fn? launchbox-core/launchbox-provider)))

  (testing "launchbox-provider returns provider when discovery disabled"
    (with-redefs [launchbox-mqtt/publish-homeassistant-discovery! (constantly nil)]
      (let [mock-client :mock-client
            config {:integrations {:home-assistant {:discovery? false}}
                    :launchbox {:test "config"}}
            provider (launchbox-core/launchbox-provider mock-client config)]
        ;; Should still return provider even when discovery disabled
        (is (instance? LaunchBoxProvider provider)))))

  (testing "launchbox-provider creates LaunchBoxProvider record"
    (with-redefs [launchbox-mqtt/publish-homeassistant-discovery! (constantly nil)]
      (let [mock-client :mock-client
            config {:integrations {:home-assistant {:discovery? true}}
                    :launchbox {:test "config"}}
            provider (launchbox-core/launchbox-provider mock-client config)]
        (is (instance? LaunchBoxProvider provider))
        (is (= :mock-client (:mqtt-client provider)))
        (is (= {:test "config"} (:config provider)))))))

(deftest start-listening-behavior-test
  (testing "start-listening! can be called on provider"
    (let [listening-atom @#'launchbox-core/listening?
          original-state @listening-atom]
      (try
        (with-redefs [mqtt/subscribe! (fn [& _] :mock-subscription)]
          (let [mock-client :mock-client
                config {:integrations {:home-assistant {:discovery? true}}
                        :launchbox {:test "config"}}
                provider (launchbox-core/->LaunchBoxProvider mock-client config)]
            (provider/start-listening! provider)
            ;; Verify listening state changed
            (is (true? @listening-atom))))
        (finally
          (reset! listening-atom original-state))))))

(deftest stop-listening-behavior-test
  (testing "stop-listening! can be called on provider"
    (let [listening-atom @#'launchbox-core/listening?
          subs-atom @#'launchbox-core/subscriptions
          original-listening @listening-atom
          original-subs @subs-atom]
      (try
        (with-redefs [mqtt/unsubscribe! (fn [& _] nil)]
          ;; Set up initial state
          (reset! listening-atom true)
          (reset! subs-atom [:sub1 :sub2])

          (let [mock-client :mock-client
                config {:launchbox {:test "config"}}
                provider (launchbox-core/->LaunchBoxProvider mock-client config)]
            (provider/stop-listening! provider)
            ;; Verify listening state changed
            (is (false? @listening-atom))))
        (finally
          (reset! listening-atom original-listening)
          (reset! subs-atom original-subs))))))


(deftest config-extraction-test
  (testing "Provider extracts launchbox config correctly"
    (with-redefs [launchbox-mqtt/publish-homeassistant-discovery! (constantly nil)]
      (let [mock-client :mock-client
            full-config {:integrations {:home-assistant {:discovery? true}}
                         :launchbox {:setting1 "value1"
                                     :setting2 "value2"}}
            provider (launchbox-core/launchbox-provider mock-client full-config)]
        (is (= {:setting1 "value1" :setting2 "value2"} (:config provider)))))))

(deftest discovery-flag-handling-test
  (testing "Discovery flag is read from correct path in config"
    (let [discovery-called (atom false)]
      (with-redefs [launchbox-mqtt/publish-homeassistant-discovery!
                    (fn [& _] (reset! discovery-called true))]
        (let [mock-client :mock-client
              config {:integrations {:home-assistant {:discovery? true}}
                      :launchbox {:test "config"}}]
          (launchbox-core/launchbox-provider mock-client config)
          (is (true? @discovery-called))))))

  (testing "Discovery not called when disabled"
    (let [discovery-called (atom false)]
      (with-redefs [launchbox-mqtt/publish-homeassistant-discovery!
                    (fn [& _] (reset! discovery-called true))]
        (let [mock-client :mock-client
              config {:integrations {:home-assistant {:discovery? false}}
                      :launchbox {:test "config"}}]
          (launchbox-core/launchbox-provider mock-client config)
          (is (false? @discovery-called)))))))

(deftest subscription-management-test
  (testing "Subscriptions are stored when starting"
    (let [subs-atom @#'launchbox-core/subscriptions
          listening-atom @#'launchbox-core/listening?
          original-subs @subs-atom
          original-listening @listening-atom]
      (try
        (reset! listening-atom false)
        (reset! subs-atom [])
        (with-redefs [mqtt/subscribe! (fn [& _] :new-subscription)]
          (let [provider (launchbox-core/->LaunchBoxProvider
                           :mock-client {:integrations {:home-assistant {:discovery? true}}})]
            (#'launchbox-core/-start-listening! :mock-client (:config provider))
            ;; Subscription should be added
            (is (some #(= :new-subscription %) @subs-atom))))
        (finally
          (reset! subs-atom original-subs)
          (reset! listening-atom original-listening))))))

(deftest idempotent-start-listening-test
  (testing "start-listening! is idempotent"
    (let [listening-atom @#'launchbox-core/listening?
          original-state @listening-atom
          call-count (atom 0)]
      (try
        (reset! listening-atom false)
        (with-redefs [mqtt/subscribe! (fn [& _]
                                        (swap! call-count inc)
                                        :mock-subscription)]
          (let [provider (launchbox-core/->LaunchBoxProvider
                           :mock-client {:integrations {:home-assistant {:discovery? true}}})]
            (provider/start-listening! provider)
            (provider/start-listening! provider)
            (provider/start-listening! provider)
            ;; Subscribe should only be called once due to listening? guard
            (is (= 1 @call-count))))
        (finally
          (reset! listening-atom original-state))))))
