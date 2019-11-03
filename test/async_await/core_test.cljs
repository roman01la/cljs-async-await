(ns async-await.core-test
  (:require [cljs.test :as test :refer [deftest is]]
            [async-await.core :refer [async await await-all await-first]]))

(deftest test-async
  (test/async done
    (-> (async 1)
        (.then #(is (= 1 %)))
        (.then #(done)))))

(deftest test-await
  (test/async done
    (async
      (let [x (await 1)]
        (is (= 1 x))
        (done)))))

(deftest test-await-all
  (test/async done
    (async
      (let [xs (await-all [1 2 3])]
        (is (= [1 2 3] xs))
        (done)))))

(deftest test-await-first
  (test/async done
    (async
      (let [x (await-first [1 2 3])]
        (is (= 1 x))
        (done)))))

(deftest test-recursive-async-fn
  (test/async done
    (let [async-fn (fn async-fn* [n]
                     (async
                       (if (pos? n)
                         (async-fn* (await (dec n)))
                         (do (is (= 0 n))
                             n))))]
      (-> (async-fn 3)
          (.then #(done))))))

(defn -main []
  (test/run-tests))

