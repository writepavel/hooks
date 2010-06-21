# Hooks

Hooks provides a flexible, composable mechanism by which you
can extend behaviour of functions after they've been defined.

Add this to your project.clj :dependencies list:

    [hooks "1.0.0"]

If you would like to make your software extensible using Hooks, all
you need to do is provide a convention for namespaces that will get
loaded on startup. Then users can place files that call add-hook under
a specific namespace prefix (my.program.hooks.*) which they can rely
on getting loaded at startup.

Hooks is inspired by Emacs Lisp's defadvice, clojure.test fixtures and
CLOS multimethods.

## Usage

    (use 'hooks)

    (defn examine [x]
      (print x))

    (defn microscope [f x]             ;; x -> X
      (f (.toUpperCase x)))

    (defn doubler [f & xs]             ;; x -> xx
      (apply f xs)
      (apply f xs))

    (defn telescope [f x]              ;; xx -> x x
     (f (apply str (interpose " " x))))

    (defn into-string [f & xs]
      (with-out-str (apply f xs)))

    (add-hook :around #'examine :microscope microscope)
    (add-hook :around #'examine :doubler    doubler)
    (add-hook :around #'examine :telescope  telescope)
    (add-hook :around #'examine :into-str   into-string)
    (add-hook :after  #'examine :dotspace   (fn [& args] (print \. \space)))
    
    (examine "Before i forget")
    > B E F O R E   I   F O R G E T.  B E F O R E   I   F O R G E T.  
    
    (remove-hook :after  #'examine :dotspace)
    (remove-hook :around #'examine :doubler)
    
    (examine "Before i forget")
    > B E F O R E   I   F O R G E T
    
    (remove-hook :around #'examine :microscope)
    
    (examine "Before i forget")
    > B e f o r e   i   f o r g e t
    
    (remove-hook :around #'examine :telescope)
    
    (examine "Before i forget")
    > Before i forget

Hooks are functions that wrap other functions. They receive the
original function and its arguments as their arguments. Hook
functions can wrap the target functions in binding, change the
argument list, only run the target functions conditionally, or all
sorts of other stuff.

Technically the first argument to a hook function is not always the
target function; if there is more than one hook then the first hook
will receive a function that is a composition of the remaining
hooks. But when you're writing hooks, you should act as if it is the
target function.

## License

Copyright (C) 2010 Phil Hagelberg, Kevin Downey and Roman Zaharov

Distributed under the Eclipse Public License, the same as Clojure.
