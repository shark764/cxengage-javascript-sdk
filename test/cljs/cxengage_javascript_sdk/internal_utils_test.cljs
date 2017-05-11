(ns cxengage-javascript-sdk.internal-utils-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs.core.async :as a]
            [cljs.spec :as s]
            [cljs-uuid-utils.core :as uuid]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.core :as m]
            [cljs.test :refer-macros [deftest is testing run-tests async use-fixtures]]))

(deftest build-url-test
  (testing "build url - testing string replace"
    (is (= "/v1/tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
           (iu/build-api-url-with-params
            "/v1/tenants/tenant-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"})))

    (is (= "/v1/tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/presence/55fb415a-1202-49d7-9fcd-be7476a439da/direction"
           (iu/build-api-url-with-params
            "/v1/tenants/tenant-id/presence/resource-id/direction"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :resource-id "55fb415a-1202-49d7-9fcd-be7476a439da"})))

    (is (= "/v1/tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/users/55fb415a-1202-49d7-9fcd-be7476a439da/session/4adbbeef-32d0-4760-8402-633c1da9f619"
           (iu/build-api-url-with-params
            "/v1/tenants/tenant-id/users/resource-id/session/session-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :resource-id "55fb415a-1202-49d7-9fcd-be7476a439da"
             :session-id "4adbbeef-32d0-4760-8402-633c1da9f619"})))

    (is (= "tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/queues/e71ffee3-6cf4-40d0-8a8d-1c281e68e030"
           (iu/build-api-url-with-params
            "tenants/tenant-id/queues/entity-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :entity-id "e71ffee3-6cf4-40d0-8a8d-1c281e68e030"})))

    (is (= "tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/interactions/e71ffee3-6cf4-40d0-8a8d-1c281e68e030/notes/d3bdcb02-9822-48c9-a2c0-d2c7160619ca"
           (iu/build-api-url-with-params
            "tenants/tenant-id/interactions/entity-id/notes/entity-sub-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :entity-id "e71ffee3-6cf4-40d0-8a8d-1c281e68e030"
             :entity-sub-id "d3bdcb02-9822-48c9-a2c0-d2c7160619ca"})))

    (is (= "tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/contacts/c8ceeec6-db54-4f77-8b7a-f2f7883fbd31"
           (iu/build-api-url-with-params
            "tenants/tenant-id/contacts/contact-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :contact-id "c8ceeec6-db54-4f77-8b7a-f2f7883fbd31"})))

    (is (= "tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/contacts/layouts/b1de66e7-edf4-49fe-8934-44c369747b75"
           (iu/build-api-url-with-params
            "tenants/tenant-id/contacts/layouts/layout-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :layout-id "b1de66e7-edf4-49fe-8934-44c369747b75"})))

    (is (= "tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/interactions/ea10fdc7-47f4-482e-81a8-4208640c7510"
           (iu/build-api-url-with-params
            "tenants/tenant-id/interactions/interaction-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :interaction-id "ea10fdc7-47f4-482e-81a8-4208640c7510"})))

    (is (= "tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/interactions/ea10fdc7-47f4-482e-81a8-4208640c7510/actions/b5892a8a-7425-4f17-8123-4f33cb5c63a8"
           (iu/build-api-url-with-params
            "tenants/tenant-id/interactions/interaction-id/actions/action-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :interaction-id "ea10fdc7-47f4-482e-81a8-4208640c7510"
             :action-id "b5892a8a-7425-4f17-8123-4f33cb5c63a8"})))

    (is (= "tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/interactions/ea10fdc7-47f4-482e-81a8-4208640c7510/artifacts/97cc2126-7a94-495f-adfc-b112da9cdfb2"
           (iu/build-api-url-with-params
            "tenants/tenant-id/interactions/interaction-id/artifacts/artifact-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :interaction-id "ea10fdc7-47f4-482e-81a8-4208640c7510"
             :artifact-id "97cc2126-7a94-495f-adfc-b112da9cdfb2"})))

    (is (= "tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/interactions/ea10fdc7-47f4-482e-81a8-4208640c7510/notes/dd8b3045-eb17-403c-9c2e-e7a86bff6f69"
           (iu/build-api-url-with-params
            "tenants/tenant-id/interactions/interaction-id/notes/note-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :interaction-id "ea10fdc7-47f4-482e-81a8-4208640c7510"
             :note-id "dd8b3045-eb17-403c-9c2e-e7a86bff6f69"})))

    (is (= "/v1/tenants/a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d/interactions/ea10fdc7-47f4-482e-81a8-4208640c7510/artifacts/6624b161-085f-4bc0-935c-6e1463cad5f2"
           (iu/build-api-url-with-params
            "/v1/tenants/tenant-id/interactions/interaction-id/artifacts/artifact-file-id"
            {:tenant-id "a4a4046c-fc8d-4c18-b058-dd4e8f8cd06d"
             :interaction-id "ea10fdc7-47f4-482e-81a8-4208640c7510"
             :artifact-file-id "6624b161-085f-4bc0-935c-6e1463cad5f2"})))))
