(ns ^{:doc "Parser for basic Pabu syntactic elements."
      :author "Paula Gearon"}
  naga.lang.basic
  (:refer-clojure :exclude [char])
  (:require [the.parsatron :refer :all]))

(defn choice* 
  "choice with backtracking."
  [& args]
  (apply choice (map attempt args)))

(defn either*
  "either with backtracking."
  [p q]
  (either (attempt p) (attempt q)))

(def non-star (token (complement #{\*})))
(def non-slash (token (complement #{\/})))

;; parser that looks for comments of the form:  /* the comment */
(defparser cmnt []
  (let->> [_ (>> (string "/*") (many non-star) (many1 (char \*)))
           _ (many (>> non-slash (many non-star) (many1 (char \*))))
           _ (char \/)]
    (always :cmnt)))

;; parsers for various single characters, etc
(def whitespace-char (token #{\space \newline \tab}))
(def opt-whitespace (many (either whitespace-char (cmnt))))
(def separator (>> opt-whitespace (char \,) opt-whitespace))
(def open-paren (>> (char \() opt-whitespace))
(def close-paren (>> opt-whitespace (char \))))

(def equals (char \=))
(def not-equals (string "!="))
(def lt (char \<))
(def gt (char \>))
(def lte (string "<="))
(def gte (string ">="))

(def plus (char \+))
(def minus (char \-))
(def divide (char \/))
(def tms (char \*))

(defn upper-case-letter?
  "Prolog considers underscores to be equivalent to an uppercase letter"
  [c]
  (or (Character/isUpperCase ^Character c) (= \_ c)))

;; parser for upper-case letters
(defn upper-case-letter
  []
  (token upper-case-letter?))

(def word (many1 (letter)))

(def digits (many1 (digit)))

(defparser signed-digits []
  (let->> [s (token #{\+ \-})
           ds digits]
    (always (cons s ds))))

(defparser integer []
  (let->> [i (either digits (signed-digits))]
    (always (Long/parseLong (apply str i)))))

(defparser floating-point []
  (let->> [i (either digits (signed-digits))
           f (>> (char \.) (many1 (digit)))]
    (always (Double/parseDouble (apply str (apply str i) \. f)))))

(def number (either* (floating-point) (integer)))

;; parses strings of the form: 'it''s a string!'
(defparser pstring1 []
  (let->> [s (many1 (between (char \') (char \') (many (any-char)))) ]
    (always (flatten (interpose \' s)))))

;; parses strings of the form: "She said, ""Hello,"" to me."
(defparser pstring2 []
  (let->> [s (many1 (between (char \") (char \") (many (any-char))))]
    (always (flatten (interpose \" s)))))

(def pstring (either (pstring1) (pstring2)))

;; variables start with a capital. Internally they start with ?
(defparser variable []
  (let->> [f (upper-case-letter)
           r (many (letter))]
    (always (symbol (apply str "?" (Character/toLowerCase f) r) ))))

;; atomic values, like a predicate, are represented as a keyword
(defparser kw []
  (let->> [r word]
    (always (keyword (apply str r)))))

;; an atom is a atomic value, a number or a string
(defparser atm []
  (choice (kw) pstring number))

;; elements in a statement are atoms or a variable
(defparser elt []
  (choice (variable) (atm)))

(defparser arg-list []
  (let->> [f (elt)
           r (many (>> separator (elt)))]
    (always (cons f r))))
