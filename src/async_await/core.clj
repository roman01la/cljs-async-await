(ns async-await.core
  (:refer-clojure :exclude [await])
  (:require [cljs.analyzer :as ana]
            [cljs.compiler :as compiler]))

(def ^:dynamic *in-async* false)

(alter-var-root #'ana/specials #(conj % 'async* 'await*))

(defmethod ana/parse 'await*
  [op env [_ expr :as form] _ _]
  (when-not *in-async*
    (throw (ana/error env "Can't await outside of async block")))
  (when (not= 2 (count form))
    (throw (ana/error env "Wrong number of args to await")))
  {:env env
   :op :await
   :children [:expr]
   :expr (ana/analyze env expr)
   :form form})

(defmethod ana/parse 'async*
  [op env [_ & exprs :as form] _ _]
  (binding [*in-async* true]
    (let [statements (ana/disallowing-recur
                       (->> (butlast exprs)
                            (mapv #(ana/analyze (assoc env :context :statement) %))))
          ret (ana/disallowing-recur
                (ana/analyze (assoc env :context :return) (last exprs)))
          children [:statements :ret]]
      {:op :async
       :env env
       :form form
       :statements statements
       :ret ret
       :ret-tag 'js/Promise
       :children children})))

(defmethod compiler/emit* :await
  [{:keys [env expr]}]
  (when (= :return (:context env))
    (compiler/emits "return "))
  (compiler/emits "(await ")
  (compiler/emits (assoc-in expr [:env :context] :expr))
  (compiler/emits ")"))

(defmethod compiler/emit* :async
  [{:keys [statements ret env]}]
  (when (= :return (:context env))
    (compiler/emits "return "))
  (compiler/emitln "(async function (){")
  (doseq [s statements]
    (compiler/emitln s))
  (compiler/emit ret)
  (compiler/emitln "})()"))

;; ====== Public API ======

(defmacro async
  "Wraps body into self-invoking JavaScript's async function, returns promise"
  [& body]
  `(~'async* ~@body))

(defmacro await
  "Suspends execution of current async block and returns asynchronously resolved value"
  [expr]
  `(~'await* ~expr))

(defmacro await-all
  "Same as (seq (.all js/Promise coll)), but for easier usage within async blocks"
  [coll]
  `(seq (~'await* (.all js/Promise ~coll))))

(defmacro await-first
  "Same as (.race js/Promise coll), but for easier usage within async blocks"
  [coll]
  `(~'await* (.race js/Promise ~coll)))
