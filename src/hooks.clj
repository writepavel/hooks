(ns hooks)

(defmacro with-return [val & body]
  `(let [return# (do ~val)]
     (do ~@body)
     return#))

(defn- compose-hooks [f1 f2]
  (fn [& args]
    (apply f2 f1 args)))

(defn- join-hooks [original hooks]
  (reduce compose-hooks original hooks))

(defn- hooks [target]
  (::hooks (meta target)))

(defn- hooked? [target]
  (instance? clojure.lang.Ref (hooks target)))

(defn- remove-hook-from-fn [qualifier target key]
  (if-not (hooked? target) target
          (do (dosync (alter (hooks target) assoc qualifier
                             (remove #(= key (:key %))
                                     (qualifier @(hooks target)))))
              (cond (let [hooks @(hooks target)]
                      (every? empty? [(:around hooks)
                                      (:before hooks)
                                      (:after  hooks)]))
                    (:original @(hooks target))

                    :has-hooks target))))

(defn- add-hook-to-fn
  ([original]
     (let [hooks (ref {:original original
                       :before   ()
                       :after    ()
                       :around   ()})]
       (with-meta
         (fn [& args]
           (apply (join-hooks (fn [& args]
                                (doseq [hook (reverse (:before @hooks))]
                                  (apply (:fn hook) args))
                                (with-return (apply original args)
                                  (doseq [hook (:after @hooks)]
                                    (apply (:fn hook) args))))

                              (reverse (map :fn (:around @hooks))))
                  args))
         (assoc (meta original) ::hooks hooks))))

  ([qualifier target key hook]
     (remove-hook-from-fn qualifier target key)
     (let [target (if-not (hooked? target) (add-hook-to-fn target) target)]
       (with-return target
         (dosync (alter (hooks target) assoc qualifier
                        (conj (qualifier @(hooks target))
                              {:key key :fn hook})))))))

(defn- remove-hook-from-var [qualifier target key]
  (alter-var-root target #(remove-hook-from-fn qualifier % key)))

(defn- add-hook-to-var [qualifier target key hook]
  (alter-var-root target #(add-hook-to-fn qualifier % key hook)))

(defn remove-hook [qualifier target key]
  (cond (fn? target)   (remove-hook-from-fn qualifier target key)
        (var? target) (remove-hook-from-var qualifier target key)))

(defn add-hook [qualifier target key hook]
  (cond (fn? target)   (add-hook-to-fn qualifier target key hook)
        (var? target) (add-hook-to-var qualifier target key hook)))
