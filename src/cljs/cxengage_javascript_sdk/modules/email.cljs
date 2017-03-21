(ns cxengage-javascript-sdk.modules.email
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [promesa.core :as prom :refer [promise all then]]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [ajax.core :as ax :refer [POST]]))

(s/def ::get-attachment-params
  (s/keys :req-un [::specs/interaction-id ::specs/artifact-id ::specs/artifact-file-id]
          :opt-un [::specs/callback]))

(defn get-attachment-url
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-attachment-url module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [interaction-id artifact-file-id artifact-id callback]} params
         tenant-id (state/get-active-tenant-id)
         url (str (state/get-base-api-url) "tenants/tenant-id/interactions/interaction-id/artifacts/artifact-id")
         url (iu/build-api-url-with-params
              url
              {:tenant-id tenant-id
               :interaction-id interaction-id
               :artifact-id artifact-id})
         req {:url url
              :method :get}]
     (go (let [attachment-response (a/<! (iu/api-request req))
               {:keys [api-response status]} attachment-response
               attachment (first (filterv #(= (:artifact-file-id %) artifact-file-id) (:files api-response)))]
           (p/publish {:topics (p/get-topic :attachment-received)
                       :response attachment
                       :callback callback})))
     nil)))

(s/def ::add-attachment-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn add-attachment
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (add-attachment module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [interaction-id file callback]} params
         attachment-id (id/uuid-string (id/make-random-uuid))]
     (if-not (s/valid? ::add-attachment-params params)
       (p/publish {:topics (p/get-topic :add-attachment)
                   :response (e/invalid-args-error "invalid args")
                   :callback callback})
       (do (state/add-attachment-to-reply {:interaction-id interaction-id
                                           :attachment-id attachment-id
                                           :file file})
           (p/publish {:topics (p/get-topic :add-attachment)
                       :response {:interaction-id interaction-id :attachment-id attachment-id}
                       :callback callback})
           nil)))))

(s/def ::remove-attachment-params
  (s/keys :req-un [::specs/attachment-id ::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn remove-attachment
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (remove-attachment module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [interaction-id attachment-id callback]} params]
     (if-not (s/valid? ::remove-attachment-params params)
       (p/publish {:topics (p/get-topic :remove-attachment)
                   :response (e/invalid-args-error "invalid args")
                   :callback callback})
       (do (state/remove-attachment-from-reply {:interaction-id interaction-id
                                                :attachment-id attachment-id})
           (p/publish {:topics (p/get-topic :remove-attachment)
                       :response {:interaction-id interaction-id}
                       :callback callback})
           nil)))))

(s/def ::cc vector?)
(s/def ::bcc vector?)
(s/def ::html-body string?)
(s/def ::plain-text-body string?)
(s/def ::subject string?)
(s/def ::to vector?)

(s/def ::send-reply-params
  (s/keys :req-un [::cc ::bcc ::html-body ::plain-text-body ::subject ::to ::specs/interaction-id]
          :opt-un [::callback]))

(defn html-body-id [files]
  (:artifact-file-id (first (filter #(= (:filename %) "htmlBody") files))))

(defn plain-body-id [files]
  (:artifact-file-id (first (filter #(= (:filename %) "plainTextBody") files))))

(defn build-attachments [files]
  (filterv
   identity
   (mapv
    (fn [file]
      (if (or (= (:filename file) "htmlBody")
              (= (:filename file) "plainTextBody"))
        nil
        {:filename (:filename file)
         :headers [{:content-type (:content-type file)}]
         :artifact-file-id (:artifact-file-id file)}))
    files)))

(defn send-reply
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-reply module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [callback cc bcc html-body plain-text-body subject to interaction-id]} params
         artifact-id (state/get-reply-artifact-id-by-interaction-id interaction-id)
         tenant-id (state/get-active-tenant-id)
         artifact-url (-> (state/get-base-api-url)
                          (str "tenants/tenant-id/interactions/interaction-id/artifacts/artifact-id")
                          (iu/build-api-url-with-params {:tenant-id tenant-id
                                                         :interaction-id interaction-id
                                                         :artifact-id artifact-id}))]
     (if-not (s/valid? ::send-reply-params params)
       (p/publish {:topics (p/get-topic :send-reply)
                   :response (e/invalid-args-error "bad args")
                   :callback callback})
       (let [request-list {:html-body nil
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

                                                  :else (doto (js/FormData.) (.append (.-name file) file (.-name file))))
                                                filename (cond
                                                           (= attachment-type :plain-text-body) :plainTextBody
                                                           (= attachment-type :html-body) :htmlBody
                                                           :else (keyword (.-name file)))
                                                content-type (cond
                                                               (= attachment-type :plain-text-body) "text/plain"
                                                               (= attachment-type :html-body) "text/html"
                                                               :else (.-type file))]
                                            (iu/file-api-request
                                             {:method :post
                                              :url artifact-url
                                              :body form-data
                                              :callback (fn [{:keys [api-response status]} response]
                                                          (resolve {:artifact-file-id (get api-response filename)
                                                                    :filename (name filename)
                                                                    :content-type content-type}))}))))))
                               []
                               attachments)
             all-req-promises (all all-req-promises)]
         (then all-req-promises
               (fn [results]
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
                       manifest-string (js/JSON.stringify (iu/camelify manifest-map))
                       form-data (doto (js/FormData.)
                                   (.append "manifest.json"
                                            (js/Blob. (clj->js [manifest-string]) #js {"type" "application/json"})
                                            "manifest.json"))
                       create-manifest-request {:method :post
                                                :url artifact-url
                                                :body form-data}]
                   (go (let [manifest-response (a/<! (iu/file-api-request create-manifest-request))
                             {:keys [api-response status]} manifest-response
                             manifest-id (get api-response :manifest.json)
                             artifact-update-request {:method :put
                                                      :url artifact-url
                                                      :body {:manifest-id manifest-id
                                                             :artifactType "email"}}
                             artifact-update-response (a/<! (iu/api-request artifact-update-request))
                             flow-url (-> (state/get-base-api-url)
                                          (str "tenants/tenant-id/interactions/interaction-id/interrupts")
                                          (iu/build-api-url-with-params
                                           {:tenant-id tenant-id
                                            :interaction-id interaction-id}))
                             flow-body {:interrupt {:resource-id (state/get-active-user-id)
                                                    :artifact-id artifact-id}
                                        :interrupt-type "send-email"
                                        :source "client"}
                             flow-send-email-request {:method :post
                                                      :url flow-url
                                                      :body flow-body}
                             flow-response (a/<! (iu/api-request flow-send-email-request))
                             {:keys [api-response status]} flow-response]
                         (if (not= status 200)
                           (p/publish {:topics (p/get-topic :send-reply)
                                       :response (e/api-error "api returned error")
                                       :callback callback})
                           (p/publish {:topics (p/get-topic :send-reply)
                                       :response {:interaction-id interaction-id}
                                       :callback callback})))))))
         nil)))))

(def initial-state
  {:module-name :email})

(defrecord EmailModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)
          email-integration true]
      (if-not email-integration
        (a/put! core-messages< {:module-registration-status :failure
                                :module module-name})
        (do (register {:api {:interactions {:email {:add-attachment (partial add-attachment this)
                                                    :remove-attachment (partial remove-attachment this)
                                                    :get-attachment-url (partial get-attachment-url this)
                                                    :send-reply (partial send-reply this)}}}
                       :module-name module-name})
            (js/console.info "<----- Started " (name module-name) " module! ----->")))))
  (stop [this]))
