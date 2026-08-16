# VOL

**Value-Oriented Language**

VOL is a programming language that aims to describe programs through the **creation, connection, and evaluation of Values**.

VOL's primary goal is not to reduce functionality, but to reduce the number of concepts that make up a programming language.

## Philosophy

### Everything Is a Value

In VOL, everything is a Value whenever possible.

Numbers, strings, Bools, and Records are Values, as are functions and control flow.

```vol
age: Int = 20
name: String = "VOL"

add: fn = (a: Int, b: Int) {
    a + b
}

User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}

adult: if = (age >= 20) {
    print("adult")
}

counter: loop = (0, 10, 1) (i) {
    print(i)
}
```

`20`, `"VOL"`, functions, Records, conditionals, and loops are all treated as Values.

---

### One Declaration Form

VOL expresses as many declarations as possible using a single form.

```vol
name: type = value
```

For example:

```vol
age: Int = 20

User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}

add: fn = (a: Int, b: Int) {
    a + b
}
```

Variables, Records, and functions use the same structure instead of having separate declaration syntax.

---

### Control Flow Is a Value

In VOL, control flow is a Value rather than a special kind of statement.

```vol
check: if = (age >= 20) {
    print("adult")
}
```

This code does not execute the condition immediately. It creates an `if` Value named `check`.

```vol
check
```

When the Value is evaluated, the condition is evaluated and the appropriate code is executed. Loops work the same way.

```vol
counter: loop = (0, 10, 1) (i) {
    print(i)
}

counter
```

---

### Explicit Types, Explicit Defaults

VOL Values have explicit types and default values.

```vol
name: String = ""
age: Int = 0
enabled: Bool = false
```

Every field in a Record also defines a type and a default value.

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}
```

This minimizes implicit states in which it is unclear whether a value exists.

---

### No Built-in Null Value

VOL does not have a separate `null` Value. Instead, a Record explicitly represents its own null state.

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}
```

`null = 0` represents a normal state, while `null = 1` represents a null state.

When a Record is in the null state, that state takes precedence even if its other fields contain values.

---

## Goal

VOL's ultimate goal is to create **a language that can describe an entire program with a small number of concepts**.

Instead of adding new syntax and concepts whenever a feature is needed, VOL first considers whether the feature can be expressed using the existing Value model.

As a result, VOL aims to be easy to learn, to have few implicit rules that readers must know, and to express a wide range of programs on top of a small language model.

VOL does not merely aim to have concise syntax. It aims to be **a language that requires few concepts to understand the language itself**.

## Status

VOL is currently in the early design and prototyping stage.

Its syntax and semantics may change during implementation.
