(ns cxengage-javascript-sdk.domain.protocols)

(defprotocol SDKModule
  (start [this] "")
  (stop [this] "")
  (refresh-integration [this] ""))
