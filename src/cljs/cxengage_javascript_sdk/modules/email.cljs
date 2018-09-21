(ns cxengage-javascript-sdk.modules.email
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]]
                   [clojure.string :as str])
  (:require [cljs.core.async :as a]
            [cljs.spec.alpha :as s]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [promesa.core :as prom :refer [promise all then]]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.email.getAttachmentUrl({
;;   interactionId: {{uuid}},
;;   artifactId: {{uuid}},
;;   artifactFileId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-attachment-url-params
  (s/keys :req-un [::specs/interaction-id ::specs/artifact-id ::specs/artifact-file-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-attachment-url
  {:validation ::get-attachment-url-params
   :topic-key :attachment-received}
  [params]
  (let [{:keys [topic interaction-id artifact-file-id artifact-id callback]} params
        attachment-response (a/<! (rest/get-artifact-by-id-request artifact-id interaction-id nil))
        {:keys [api-response status]} attachment-response]
    (if (= status 200)
      (let [attachment (first
                        (filterv
                         #(= (:artifact-file-id %) artifact-file-id)
                         (:files api-response)))]
        (p/publish {:topics topic
                    :response attachment
                    :callback callback}))
      (p/publish {:topics topic
                  :error (e/failed-to-get-attachment-url-err interaction-id artifact-file-id artifact-id attachment-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.email.addAttachment({
;;   interactionId: {{uuid}},
;;   file: {{HTML5 File}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::add-attachment-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn add-attachment
  {:validation ::add-attachment-params
   :topic-key :add-attachment}
  [params]
  (let [{:keys [topic interaction-id file callback]} params
        attachment-id (id/uuid-string (id/make-random-uuid))]
    (state/add-attachment-to-reply {:interaction-id interaction-id
                                    :attachment-id attachment-id
                                    :file file})
    (p/publish {:topics topic
                :response {:interaction-id interaction-id :attachment-id attachment-id :filename (.-name file)}
                :callback callback})))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.email.removeAttachment({
;;   interactionId: {{uuid}},
;;   attachmentId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::remove-attachment-params
  (s/keys :req-un [::specs/attachment-id ::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn remove-attachment
  {:validation ::remove-attachment-params
   :topic-key :remove-attachment}
  [params]
  (let [{:keys [topic interaction-id attachment-id callback]} params]
    (state/remove-attachment-from-reply {:interaction-id interaction-id
                                         :attachment-id attachment-id})
    (p/publish {:topics topic
                :response {:interaction-id interaction-id
                           :attachment-id attachment-id}
                :callback callback})))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.email.sendReply({
;;   interactionId: {{uuid}},
;;   to: {{string}},
;;   subject: {{string}},
;;   plainTextBody: {{string}},
;;   htmlBody: {{string}},
;;   bcc: {{string}},
;;   cc: {{string}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::send-reply-params
  (s/keys :req-un [::specs/cc ::specs/bcc ::specs/html-body ::specs/plain-text-body ::specs/subject ::specs/to ::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn html-body-id [files]
  (log :debug "[Email Processing] Files when building html body id" files)
  (:artifact-file-id (first (filter #(= (:filename %) "htmlBody") files))))

(defn plain-body-id [files]
  (log :debug "[Email Processing] Files when building plain body id" files)
  (:artifact-file-id (first (filter #(= (:filename %) "plainTextBody") files))))

(defn build-attachments [files]
  (filterv
   identity
   (mapv
    (fn [file]
      (if (or (= (:filename file) "htmlBody")
              (= (:filename file) "plainTextBody"))
        nil
        (do (log :debug "file in build attachments:" file)
            {:filename (:filename file)
             :headers [{:content-type (:content-type file)}]
             :artifact-file-id (:artifact-file-id file)})))
    files)))

(def-sdk-fn send-reply
  {:validation ::send-reply-params
   :topic-key :send-reply}
  [params]
  (let [{:keys [interaction-id callback topic]} params
        artifact-id (state/get-reply-artifact-id-by-interaction-id interaction-id)]
    (if artifact-id
      (let [{:keys [cc bcc html-body plain-text-body subject to]} params
            tenant-id (state/get-active-tenant-id)
            artifact-url (iu/api-url
                          "tenants/:tenant-id/interactions/:interaction-id/artifacts/:artifact-id"
                          {:tenant-id tenant-id
                           :interaction-id interaction-id
                           :artifact-id artifact-id})
            request-list {:html-body nil
                          :plain-text-body nil}
            attachments (-> (state/get-all-reply-email-attachments interaction-id)
                            (assoc :html-body html-body)
                            (assoc :plain-text-body plain-text-body))
            all-req-promises (reduce-kv
                              (fn [acc id file]
                                (conj acc
                                      (promise
                                       (fn [resolve reject]
                                         (let [attachment-type (cond
                                                                 (= id :plain-text-body) :plain-text-body
                                                                 (= id :html-body) :html-body
                                                                 :else :attachment)
                                               form-data
                                               (cond
                                                 (= id :plain-text-body)
                                                 (doto (js/FormData.)
                                                   (.append "plainTextBody"
                                                            (js/Blob. (clj->js [plain-text-body]) #js {"type" "text/plain"})
                                                            "plainTextBody"))

                                                 (= id :html-body)
                                                 (doto (js/FormData.)
                                                   (.append "htmlBody"
                                                            (js/Blob. (clj->js [html-body]) #js {"type" "text/html"})
                                                            "htmlBody"))

                                                 :else (do (log :debug "[Email Processing] Name property on attachment file passed:" (.-name file))
                                                           (log :debug "[Email Processing] File passed:" file)
                                                           (doto (js/FormData.) (.append (.-name file) file (.-name file)))))

                                               filename (cond
                                                          (= attachment-type :plain-text-body) :plainTextBody
                                                          (= attachment-type :html-body) :htmlBody
                                                          :else (keyword (.-name file)))
                                               content-type (cond
                                                              (= attachment-type :plain-text-body) "text/plain"
                                                              (= attachment-type :html-body) "text/html"
                                                              :else (.-type file))]
                                           (log :debug "[Email Processing] File name:" filename)
                                           (log :debug "[Email Processing] File content-type:" content-type)
                                           (log :debug "[Email Processing] Form data:" form-data)
                                           (log :debug "[Email Processing] Attachment Type:" attachment-type)
                                           (log :debug "[Email Processing] Artifact URL:" artifact-url)
                                           (rest/file-api-request
                                            {:method :post
                                             :url artifact-url
                                             :body form-data
                                             :callback (fn [{:keys [api-response status]} response]
                                                         (log :debug "[Email Processing] API response for file upload:" api-response)
                                                         (log :debug "[Email Processing] Filename from API response" filename)
                                                         (log :debug "[Email Processing] String'd filename:" (name filename))
                                                         (resolve {:artifact-file-id (first (vals api-response))
                                                                   :filename (name filename)
                                                                   :content-type content-type}))}))))))
                              []
                              attachments)
            all-req-promises (all all-req-promises)]
        (then all-req-promises
              (fn [results]
                (log :debug "[Email Processing] Done ALL file uploads for the email reply artifact. All attachments + html body + plain text body. Upload response:" (js/JSON.stringify (ih/camelify results) nil 2))
                (let [in-reply-to-id (state/get-email-reply-to-id interaction-id)
                      manifest-map {:attachments (build-attachments results)
                                    :cc cc
                                    :bcc bcc
                                    :headers []
                                    :in-reply-to-id in-reply-to-id
                                    :body {:html {:artifact-file-id (html-body-id results)}
                                           :plain {:artifact-file-id (plain-body-id results)}}
                                    :subject subject
                                    :to to}
                      manifest-string (js/JSON.stringify (ih/camelify manifest-map))
                      _ (log :debug "[Email Processing] Manifest we're creating:" manifest-string)
                      form-data (doto (js/FormData.)
                                  (.append "manifest.json"
                                           (js/Blob. (clj->js [manifest-string]) #js {"type" "application/json"})
                                           "manifest.json"))
                      create-manifest-request {:method :post
                                               :url artifact-url
                                               :body form-data}]
                  (go (let [manifest-response (a/<! (rest/file-api-request create-manifest-request))
                            _ (log :debug "[Email Processing] Manifest creation response:" (ih/camelify manifest-response))
                            {:keys [api-response status]} manifest-response
                            manifest-id (get api-response :manifest.json)
                            artifact-update-body {:manifest-id manifest-id
                                                  :artifactType "email"}
                            artifact-update-response (a/<! (rest/update-artifact-request artifact-update-body artifact-id interaction-id))
                            _ (log :debug "[Email Processing] Artifact update response:" (ih/camelify artifact-update-response))
                            interrupt-type "send-email"
                            interrupt-body {:resource-id (state/get-active-user-id)
                                            :artifact-id artifact-id}
                            flow-response (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))
                            {:keys [api-response status]} flow-response]
                        (if (= status 200)
                          (p/publish {:topics topic
                                      :response {:interaction-id interaction-id}
                                      :callback callback})
                          (p/publish {:topics topic
                                      :error (e/failed-to-send-email-reply-err interaction-id flow-response)
                                      :callback callback}))))))))
      (p/publish {:topics topic
                  :error (e/failed-to-send-email-reply-no-artifact-err interaction-id artifact-id)
                  :callback callback}))))

(s/def ::start-outbound-email-params
  (s/keys :req-un [::specs/address]
          :opt-un [::specs/callback]))

(def-sdk-fn start-outbound-email
  {:validation ::start-outbound-email-params
   :topic-key :start-outbound-email}
  [params]
  (let [{:keys [address callback topic]} params
        resource-id (state/get-active-user-id)
        interaction-id (str (id/make-random-squuid))
        session-id (state/get-session-id)
        interaction-body {:source "email"
                          :customer address
                          :contact-point "outbound-email"
                          :channel-type "email"
                          :direction "agent-initiated"
                          :interaction {:resource-id resource-id
                                        :session-id session-id}
                          :metadata {}
                          :id interaction-id}
        {:keys [api-response status] :as interaction-response} (a/<! (rest/create-interaction-request interaction-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics (topics/get-topic :failed-to-create-outbound-email-interaction)
                  :error (e/failed-to-create-outbound-email-interaction-err interaction-body interaction-id interaction-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.email.agentReplyStarted({
;;   interactionId: {{uuid}},
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::agent-reply-started-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn agent-reply-started
  {:validation ::agent-reply-started-params
   :topic-key :agent-reply-started-acknowledged}
  [params]
  (let [{:keys [interaction-id callback topic]} params
        interrupt-type "agent-reply-started"
        interrupt-body {:resource-id (state/get-active-user-id)
                        :tenant-id (state/get-active-tenant-id)
                        :session-id (state/get-session-id)
                        :channel-type "email"}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-send-agent-reply-started-err interaction-id interrupt-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.email.agentNoReply({
;;   interactionId: {{uuid}},
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::agent-no-reply-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn agent-no-reply
  {:validation ::agent-no-reply-params
   :topic-key :agent-no-reply-acknowledged}
  [params]
  (let [{:keys [interaction-id callback topic]} params
        interrupt-type "agent-no-reply"
        interrupt-body {:resource-id (state/get-active-user-id)
                        :tenant-id (state/get-active-tenant-id)
                        :session-id (state/get-session-id)
                        :channel-type "email"}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-send-agent-no-reply-err interaction-id interrupt-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.email.agentCancelledReply({
;;   interactionId: {{uuid}},
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::agent-cancel-reply-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn agent-cancel-reply
  {:validation ::agent-cancel-reply-params
   :topic-key :agent-cancel-reply-acknowledged}
  [params]
  (let [{:keys [interaction-id callback topic]} params
        interrupt-type "agent-cancel-reply"
        interrupt-body {:resource-id (state/get-active-user-id)
                        :tenant-id (state/get-active-tenant-id)
                        :session-id (state/get-session-id)
                        :channel-type "email"}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-send-agent-cancel-reply-err interaction-id interrupt-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; SDK Entities Module
;; -------------------------------------------------------------------------- ;;

(defrecord EmailModule []
  pr/SDKModule
  (start [this]
    (let [module-name :email]
      (ih/register {:api {:interactions {:email {:add-attachment add-attachment
                                                 :remove-attachment remove-attachment
                                                 :get-attachment-url get-attachment-url
                                                 :send-reply send-reply
                                                 :start-outbound-email start-outbound-email
                                                 :agent-reply-started agent-reply-started
                                                 :agent-no-reply agent-no-reply
                                                 :agent-cancelled-reply agent-cancel-reply}}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
