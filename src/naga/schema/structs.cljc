(ns naga.schema.structs
  (:require #?(:clj [schema.core :as s]
               :cljs [schema.core :as s :include-macros true])
              [naga.util :as u]))

;; single element in a rule
(def EntityPropertyElt
  (s/cond-pre s/Keyword s/Symbol #?(:clj Long :cljs s/Num)))

;; simple pattern containing a single element. e.g. [?v]
(def EntityPattern [(s/one s/Symbol "entity")])

;; two or three element pattern.
;; e.g. [?s :property]
;;      [:my/id ?property ?value]
(def EntityPropertyPattern
  [(s/one EntityPropertyElt "entity")
   (s/one EntityPropertyElt "property")
   (s/optional s/Any "value")])

;; The full pattern definition, with 1, 2 or 3 elements
(def EPVPattern
  (s/if #(= 1 (count %))
    EntityPattern
    EntityPropertyPattern))

(s/defn vartest? :- s/Bool
  [x]
  (and (symbol? x) (boolean (#{\? \%} (first (name x))))))

(s/defn vars :- [s/Symbol]
  "Return a seq of all variables in a pattern"
  [pattern :- EPVPattern]
  (filter vartest? pattern))

(def RulePatternPair [(s/one s/Str "rule-name")
                      (s/one EPVPattern "pattern")])

;; filters are executable lists destined for eval
(def FilterPattern (s/pred list?))

(def Pattern (s/if list? FilterPattern EPVPattern))

(def Body [Pattern])
(def Head [EPVPattern])

(def ConstraintData
  {:last-count s/Num  ;; The count from the previous execution
   :dirty s/Bool})    ;; If the constraint resolution is dirty

(def StatusMap {EPVPattern (s/atom ConstraintData)})

(def StatusMapEntry
  "Convenience for representing a single key/value pair in a StatusMap"
  [(s/one EPVPattern "Pattern from rule body")
   (s/one (s/atom ConstraintData) "count_and_dirty")])

(def Value (s/pred (complement symbol?) "Value"))

(def Results [[Value]])

;; Rules defined by a horn clause. The head is a simple pattern,
;; the body is conjunction of pattern matches.
;; All rules have a name, and a list of names of downstream rules.
(s/defrecord Rule
    [head :- Head
     body :- Body
     name :- s/Str
     downstream :- [RulePatternPair]
     salience :- s/Num])

(s/defrecord DynamicRule
    [head :- Head
     body :- Body
     name :- s/Str
     downstream :- [RulePatternPair]
     salience :- s/Num
     status :- {EPVPattern (s/atom ConstraintData)}
     execution-count :- (s/atom s/Num)])

(s/defn new-rule
  ([head :- Head
    body :- Body
    name :- s/Str]
   (new-rule head body name []))
  ([head :- Head
    body :- Body
    name :- s/Str
    downstream :- [RulePatternPair]]
   (new-rule head body name downstream 0))
  ([head :- Head
    body :- Body
    name :- s/Str
    downstream :- [RulePatternPair]
    salience :- s/Num]
   (->Rule head body name downstream salience))
  ([head :- Head
    body :- Body
    name :- s/Str
    downstream :- [RulePatternPair]
    salience :- s/Num
    status :- {EPVPattern (s/atom ConstraintData)}
    execution-count :- (s/atom s/Num)]
   (->DynamicRule head body name downstream salience status execution-count)))

(def EntityPropAxiomElt
  (s/cond-pre s/Keyword #?(:clj Long :cljs s/Num)))

(def EntityPropValAxiomElt
  (s/conditional (complement symbol?) s/Any))

(def Triple
  [(s/one s/Any "entity")
   (s/one s/Any "property")
   (s/one s/Any "value")])

(def Axiom
  [(s/one EntityPropAxiomElt "entity")
   (s/one EntityPropAxiomElt "property")
   (s/one EntityPropValAxiomElt "value")])

(def Statement (s/cond-pre Axiom Rule))

(def Program
  {(s/required-key :rules) {s/Str Rule}
   (s/required-key :axioms) [Axiom]})

(def RunnableProgram
  {(s/required-key :rules) {s/Str DynamicRule}
   (s/required-key :axioms) [Axiom]})