## What is this?
Experimental ClojureScript's compiler extension that enables JavaScript's async/await through `async` blocks and `await, await-all & await-first` operators.

Requires `:language-in` compiler option set to `:ecmascript-2017` or newer ECMAScript versions, this will set `:language-out` to the same version. When targeting JavaScript runtimes where `async/await` is not supported, set `:language-out` to appropriate version and Google Closure Compiler will compile it down into supported implementation.

## Usage
```clojure
(require '[async-await.core :refer [async await]])

(defn http-get [url]
  (async
    (let [response (await (js/fetch url))
          json (await (.json response))]
      (.log js/console json))))
```

## Limitations
JavaScript's `await` statement is not allowed in non-async functions, this causes a problem because of ClojureScript's codegen. When translated into JavaScript, constructs like the one below emit self-invoking function in place of `do`.
```clojure
(async
  (let [x (do (inc 1) (await 2))]
    x))
```

Output:
```javascript
(async function() {
  var x = (function() {
    1 + 1;
    return await 2;
  })();
  return x;
})()
```

## API
- `async` wraps body into self-invoking JavaScript's async function, returns promise
- `await` suspends execution of current async block and returns asynchronously resolved value
- `await-all` same as `(seq (.all js/Promise coll))`, but for easier usage within async blocks
- `await-first` same as `(.race js/Promise coll)`, but for easier usage within async blocks

## Tests
```
clojure -A:test -m cljs.main -m async-await.core-test -re node
```
