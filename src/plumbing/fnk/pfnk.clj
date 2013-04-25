(ns plumbing.fnk.pfnk
  "Core protocol for keyword functions that store input and output schema metadata about
   their arguments and return values.  This protocol says nothing about how fnks are created,
   so users are free to create PFnks directly using fn->fnk, or using custom binding syntax
   (of which our 'fnk' et al are one possible example)."
  (require [plumbing.fnk.schema :as schema]))

(defprotocol PFnk
  "Protocol for keyword functions and their specifications, e.g., fnks and graphs."  
  (io-schemata [this]
    "Return a pair of [input-schema output-schema], as specified in plumbing.fnk.schema.")
  (positional-fn [this]
    "Return the positional form of the function, which may be more
    efficient to call. If there is no such form, return nil.")
  (positional-args [this]
    "Return the arguments of the positional-fn of the function. If there
    is no such form, return nil."))

(defn input-schema [x] (first (io-schemata x)))
(defn output-schema [x] (second (io-schemata x)))

;; A fn with io-schema metadata is the simplest kind of PFnk.
(extend-type clojure.lang.Fn
  PFnk
  (io-schemata [this] 
    (let [schemata (get (meta this) ::io-schemata)]
      (when-not (= (count schemata) 2)
        (throw (RuntimeException. (format "Missing or malformed io-schemata metadata in %s" (meta this)))))
      schemata))
  (positional-fn [this]
    (get (meta this) ::positional-fn))
  (positional-args [this]
    (get (meta this) ::positional-args)))


(defn fn->fnk
  "Make a keyword function into a PFnk, by associating input and output schema metadata."
  ([f [input-schema output-schema :as io]]
   (vary-meta f assoc
              ::io-schemata io))
  ([f [input-schema output-schema :as io] positional-f positional-args]
   (vary-meta f assoc
              ::io-schemata io
              ::positional-fn positional-f
              ::positional-args positional-args)))

(defn comp-partial 
  "Return a new pfnk representing the composition #(f (merge % (other %)))"
  [f other] 
  (fn->fnk
   (fn [m] (f (merge m (other m))))
   (schema/compose-schemata (io-schemata f) (io-schemata other))))
