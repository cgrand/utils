# net.cgrand.utils

This is a collection of common functions/macros I keep rewriting from project
to project. 

## reduce-by

reduce-by is a mix of group-by and reduce: it acts like group-by but instead
of accumulating same-key elements in a vector they are reduced on the fly.

    (reduce-by key-fn conj [] coll) is equivalent to (group-by key-fn coll).

    (reduce-by identity (fn [n _] (inc n)) 0 coll) is equivalent to (frequencies coll)

## if-let and when-let

When it comes to if-let I'm of the conjunction camp: those who think that
if-let should allow to create several bindings from several test exprs and
only executes the then clause if all tests are true.

    (if-let [a (get m "a")
             b (get m "b")]
      (str "a and b " a b)
      "not (a and b)")
      

However I agree that the other camp (those who think that only the last
expression should be tested) is right from time to time. So one can introduce
new locals using :let (much with for/doseq)

    (if-let [:let [a (get m "a")]
             b (get m "b")]
      (str "b " b)
      "not b")
      
when-let is redefined to use the new if-let.

One can add new operators to if-let by doing:

    (require '[net.cgrand.xmacros :as x])
    
    (x/defop if-let ::operator-name [expr then else]
      (macro code here))
  
For example:

    (x/defop if-let ::not 
     "optional docstring"
     [expr then else]
      `(if-not ~expr ~then ~else))

Note that the operator must be namespaced (it's enforced) to allow for safe
extension of the macro from different namespaces. The docstring is contributed
to the extended macro docstring.

## cond

This version of cond is more powerful: 
* if an odd number of forms is passed to
it the last one is considered to be the else (no need for :else -- :else is
of course still supported);
* if the test expression is :let then its "then" expression is expected to be
a binding vector whose locals are visible to the rest of the cond;
* if the test expression is :when then its "then" expression is expected to be
a test which upon failure makes the cond evaluates to nil; when the test
succeeds, the rest of the cond is evaluated as usual;
* if the test expression is :when-let then its "then" expression is expected
to be a binding expression whose expressions are tests; it's based on the new
when-let/if-let so all of their operators apply here (including :let).

One can add new operators to cond by doing:

    (require '[net.cgrand.xmacros :as x])
    
    (x/defop cond ::operator-name
     "optional docstring" 
     [expr else]
      (macro code here))

Note that the operator must be namespaced (it's enforced) to allow for safe
extension of the macro from different namespaces. The docstring is contributed
to the extended macro docstring.

# extensible macros

It's the net.cgrand.xmacros namespace. Look at the implementation of cond 
in net.cgrand.utils as an example.

## License

Copyright © 2012 Christophe Grand

Distributed under the Eclipse Public License, the same as Clojure.
