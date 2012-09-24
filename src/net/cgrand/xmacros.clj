(ns net.cgrand.xmacros
  "Safely extensible macros."
  {:author "Christophe Grand"})

(defn add-operator!
 "Extends the macro for the operator identified by keyword using f as the
  backend. The macro docstring is updated to document this operator."
  [macrovar keyword f docstring]
  (when-not (= (or (and (keyword? keyword) (not= ::catch-all keyword)
                        (namespace keyword))
                   (-> macrovar meta :ns ns-name name))
               (-> *ns* ns-name name))
    (throw (IllegalArgumentException.
             "Namespaced keywords can only be added from matching namespaces. Operators for non-keywords or non-namespaced keywords can only be added from the namespace where the macro is defined.")))
  (alter-meta! macrovar
    (fn [{exts ::extensions doc :doc core-doc ::doc :as meta}]
      (let [core-doc (or core-doc doc)
        exts (assoc exts keyword {:fn f :doc docstring})
        doc (apply str core-doc "\n\nSupported ops:" (mapcat (fn [[kw {doc :doc}]]
                                          ["\n\n" kw "\n " doc]) 
                                        (sort-by key (dissoc exts ::catch-all))))]
        (assoc meta :doc doc ::doc core-doc ::extensions exts))))
  macrovar)

(defmacro defop 
 "Extends the specified macro for the specified operator (keyword)." 
  [macroname keyword docstring? args & body]
  (let [[docstring args body] (if (string? docstring?) 
                                 [docstring? args body]
                                 [nil docstring? (cons args body)])]
    `(add-operator! (var ~macroname) ~keyword (fn ~args ~@body) ~docstring)))

(defmacro defdefault-op 
  "Defines a default operator handler for the specified macro.
   The actual keyword is passed as first argument."
  [macroname args & body]
  `(add-operator! (var ~macroname) ::catch-all (fn ~args ~@body) nil))

(defn operator-fn 
  "Returns the fn backing the operator for the macro or nil."
  [macrovar keyword]
  (get-in (meta macrovar) [::extensions keyword :fn]))

(defmacro expand-macro-op [macroname op & args]
  "Expands the op."
  `(let [macrovar# (resolve ~macroname)
         op# ~op]
     (if-let [f# (operator-fn macrovar# op#)]
       (f# ~@args)
       ((operator-fn macrovar# ::catch-all) op# ~@args))))

(defmacro expand-op 
  "Lust be called inside a macro body. Expands the op."
  [op & args]
  `(expand-macro-op (first ~'&form) ~op ~@args))



