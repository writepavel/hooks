(ns hooks)

(defmacro with-return [val & body]
  `(let [return# (do ~val)]
     (do ~@body)
     return#))

(defn- multifn? [x]
  (instance? clojure.lang.MultiFn x))

(defn- compose-hooks [f1 f2]
  (fn [& args]
    (apply f2 f1 args)))

(defn- join-hooks [original hooks]
  (reduce compose-hooks original hooks))

(defn- run-hooks [hooked original args]
  (apply (join-hooks original @(::hooks (meta hooked))) args))

(defn- make-hook [key function]
  {:key key :fn function})

(defn- get-hooks [target qualifier]
  ((::hooks (meta target)) qualifier))

(defn- get-hook [target qualifier key]
  (when-let [hooks (get-hooks target qualifier)]
    (some #(when (= key (:key %)) %) hooks)))

(defn- prepared-for-hooks? [target]
  (instance? clojure.lang.Ref (::hooks (meta target))))

(defn- prepare-for-hooks [original]
  (let [hooks (ref {:original original
                    :before   ()
                    :after    ()
                    :around   ()})
        main-block (fn [& args]
                     (doseq [hook (reverse (:before @hooks))]
                       (apply (:fn hook) args))
                     (with-return (apply original args)
                       (doseq [hook (:after @hooks)]
                         (apply (:fn hook) args))))
        around-block (join-hooks main-block
                                 (reverse (map :fn (:around @hooks))))]
    (with-meta (fn [& args] (apply around-block args))
      (assoc (meta original) ::hooks hooks))))

(defmulti remove-hook
  (fn ([qualifier target key]
        (cond (fn? target) ::fn
              (var? target) ::var))))

(defmulti add-hook
  (fn ([qualifier target key hook]
        (cond (fn? target) ::fn
              (var? target) ::var))))

(defmethod remove-hook ::fn
  [qualifier target key]
  (when (prepared-for-hooks? target)
    (let [hooks-ref (::hooks (meta target))
          hooks (qualifier @hooks-ref)]
      (with-return target
        (dosync (alter hooks-ref assoc qualifier
                       (remove #(= key (:key %)) hooks)))))))

(defmethod add-hook ::fn
  [qualifier target key hook]
  (let [target (if (prepared-for-hooks? target) target
                   (prepare-for-hooks target))]
    (remove-hook qualifier target key)
    (let [hooks-ref (::hooks (meta target))
          hooks (qualifier @hooks-ref)]
      (with-return target
        (dosync (alter hooks-ref assoc qualifier
                       (conj hooks (make-hook key hook))))))))

(defmethod remove-hook ::var
  [qualifier target key]
  (when (prepared-for-hooks? @target)
    (remove-hook qualifier @target key)
    ;; (when (empty? @(::hooks (meta @target-var)))
    ;;   (alter-var-root target-var
    ;;                   (constantly (::original (meta @target-var)))))
    ))

(defmethod add-hook ::var
  [qualifier target key hook]
  (when-not (prepared-for-hooks? @target)
    (alter-var-root target prepare-for-hooks))
  (add-hook qualifier @target key hook))
