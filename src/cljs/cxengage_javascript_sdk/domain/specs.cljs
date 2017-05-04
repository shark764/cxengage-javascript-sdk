(ns cxengage-javascript-sdk.domain.specs
  (:require [cljs.spec :as s]
            [cljs-uuid-utils.core :as id]))

(defn str-long-enough? [len st]
  (>= (.-length st) len))

(s/def ::answers map?)
(s/def ::artifact-file-id ::uuid)
(s/def ::artifact-id ::uuid)
(s/def ::attachment-id ::uuid)
(s/def ::attributes map?)
(s/def ::body string?)
(s/def ::callback (s/or :callback fn? :callback nil?))
(s/def ::contact-id ::uuid)
(s/def ::contact-ids (s/coll-of ::contact-id))
(s/def ::contact-point string?)
(s/def ::digit #{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "*" "#"})
(s/def ::direction #{"inbound" "outbound"})
(s/def ::disposition-id ::uuid)
(s/def ::extension-id string?)
(s/def ::extension-value (s/or ::uuid string?))
(s/def ::interaction-id ::uuid)
(s/def ::layout-id ::uuid)
(s/def ::level #{"debug" "info" "warn" "error" "fatal"})
(s/def ::message string?)
(s/def ::min-8-len-string (and string? (partial str-long-enough? 8)))
(s/def ::note-id ::uuid)
(s/def ::password ::min-8-len-string)
(s/def ::phone-number string?)
(s/def ::query map?)
(s/def ::queue-id ::uuid)
(s/def ::reason string?)
(s/def ::reason-id ::uuid)
(s/def ::reason-list-id ::uuid)
(s/def ::resource-id ::uuid)
(s/def ::script-id ::uuid)
(s/def ::stat-id ::uuid)
(s/def ::state string?)
(s/def ::stats map?)
(s/def ::subscription-id ::uuid)
(s/def ::tenant-id ::uuid)
(s/def ::title string?)
(s/def ::topic string?)
(s/def ::transfer-extension map?)
(s/def ::transfer-type #{"cold" "warm"})
(s/def ::username ::min-8-len-string)
(s/def ::uuid id/valid-uuid?)
(s/def ::wrapup string?)
