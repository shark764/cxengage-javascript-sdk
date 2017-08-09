(ns cxengage-javascript-sdk.domain.protocols)

(defprotocol SDKModule
  "SDK Modules are segmented areas of responsibility for the CxEngage Javascript SDK."
  (start [this] "Turn on your module and bootstrap any relevant state / listeners / loops, etc.")
  (stop [this] "Turns off your module, tidying up any state and disabling listeners / ending loops as necessary.")
  (refresh-integration [this] "If your module relies on a token-based 3rd party integration that has the potential to hit token TTL expiries, set up any token-refresh logic here."))
