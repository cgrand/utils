(ns net.cgrand.utils
  "Some functions and a collection of variations on Clojure's core macros. 
   Let's see which features end up being useful."
  {:author "Christophe Grand"}
  (:refer-clojure :exclude [cond when-let if-let])
  (:require [net.cgrand.xmacros :as x]))

(defn reduce-by
  "Returns a maps keyed by the result of key-fn on eachcelement to the 
   result of calling reduce with f (and val if provided) on same-key elements.
   (reduce-by key-fn conj [] coll) is equivalent to (group-by key-fn coll).
   (reduce-by identity (fn [n _] (inc n)) 0 coll) is equivalent to
   (frequencies coll)."
  ([key-fn f val coll]
    (persistent!
      (reduce (fn [m x]
                (let [k (key-fn x)]
                  (assoc! m k (f (m k val) x)))) (transient {}) coll)))
  ([key-fn f coll]
    (let [g (fn g [acc x]
              (if (= g acc)
                x
                (f acc x)))]
      (reduce-by key-fn g g coll))))

(defmacro if-let
 "An extensible variation on if-let where all the exprs in the bindings vector
  must be true.
  Keywords in binding position are treated as operators.
  Beware of shadowing: a subset of the bound locals may be visible in the else
  expr. Do not rely on this behaviour.

  Operators impls signature is [rhs then else] where rhs is the expression
  next to the operator in the \"binding\" form."
  ([bindings then]
    `(if-let ~bindings ~then nil))
  ([bindings then else]
    (clojure.core/if-let [[op rhs & more-bindings] (seq bindings)]
      (x/expand-op op rhs `(if-let ~(vec more-bindings) ~then ~else) else)
      then)))

(x/defdefault-op if-let [binding expr then else]
  `(let [test# ~expr]
     (if test#
       (let [~binding test#]
         ~then)
       ~else)))

(x/defop if-let :let
  "Introduces local bindings, expressions are not tested to be true."
  [bindings then _]
  `(let ~bindings ~then))

(defmacro when-let
  "A variation on when-let where all the exprs in the bindings vector must be true.
   Also supports :let."
  [bindings & body]
  `(if-let ~bindings (do ~@body)))

(defmacro cond 
 "An extensible variation on cond. Trailing else is supported.
  keywords in test position are treated as operators.

  Operators impls signature is [rhs else] where rhs is the \"then\" 
  expression next to the operator." 
  [& clauses]
  (when-let [[op rhs & more-clauses] (seq clauses)]
    (if (next clauses)
      (x/expand-op op rhs `(cond ~@more-clauses))
      test)))

(x/defdefault-op cond [test-expr then else]
  (if (vector? test-expr)
    `(if-let ~test-expr ~then ~else)
    `(if ~test-expr ~then ~else)))

(x/defop cond :let 
  "Introduces local bindings."
  [bindings cont]
  `(let ~bindings ~cont))

(x/defop cond :when
  "Short-circuits the rest of the cond if false"
  [test-expr cont]
  `(when ~test-expr ~cont))

(x/defop cond :when-let
 "Short-circuits the rest of the cond if false and introduces local
  bindings."
  [bindings cont]
  `(when-let ~bindings ~cont))

(x/defop cond :else
 [else _]
 else)