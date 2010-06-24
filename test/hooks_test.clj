(ns hooks-test
  (:use :reload hooks)
  (:use clojure.test))

(defn examine [x]
  (print x))

(defn microscope [f x]
  (f (.toUpperCase x)))

(defn doubler [f & xs]
  (apply f xs)
  (apply f xs))

(defn telescope [f x]
  (f (apply str (interpose " " x))))

(defn into-string [f & xs]
  (with-out-str (apply f xs)))

(deftest add-hook-to-fn-test
  (let [e (add-hook :around examine :microscope microscope)]
    (add-hook :around e :doubler    doubler)
    (add-hook :around e :telescope  telescope)
    (add-hook :around e :into-str   into-string)
    (add-hook :after  e :dotspace   (fn [& args] (print \. \space)))
    (is (= (e "Lithp")
           "L I T H P.  L I T H P.  "))
    
    (remove-hook :after e :dotspace)
    (remove-hook :around e :doubler)
    (is (= (e "Lithp")
           "L I T H P"))
    (remove-hook :around e :telescope)
    (remove-hook :around e :microscope)
    (is (= examine (remove-hook :around e :into-str)))))

(deftest add-hook-to-var-test
  (let [unhooked-examine examine]
    (add-hook :around #'examine :microscope microscope)
    (add-hook :around #'examine :doubler    doubler)
    (add-hook :around #'examine :telescope  telescope)
    (add-hook :around #'examine :into-str   into-string)
    (add-hook :after  #'examine :dotspace   (fn [& args] (print \. \space)))
    (is (= (examine "Lithp")
           "L I T H P.  L I T H P.  "))

    (remove-hook :after  #'examine :dotspace)
    (remove-hook :around #'examine :doubler)
    (is (= (examine "Lithp")
           "L I T H P"))
  
    (remove-hook :around #'examine :microscope)
    (remove-hook :around #'examine :telescope)
    (is (not= unhooked-examine examine))
    (remove-hook :around #'examine :into-str)
    (is (= unhooked-examine examine))
    ))

(deftest simple-add-and-remove-test
  (let [unhooked-examine examine]
    (add-hook #'examine microscope)
    (add-hook #'examine doubler)
    (add-hook #'examine telescope)
    (add-hook #'examine into-string)
    (is (= (examine "Lithp")
           "L I T H PL I T H P"))

    (remove-hook #'examine doubler)
    (is (= (examine "Lithp")
           "L I T H P"))
  
    (remove-hook #'examine microscope)
    (remove-hook #'examine telescope)
    (is (not= unhooked-examine examine))
    (remove-hook #'examine into-string)
    (is (= unhooked-examine examine))
    ))
