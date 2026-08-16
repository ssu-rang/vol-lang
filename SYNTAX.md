# VOL Syntax

This document defines the syntax and basic semantics of VOL (Value-Oriented Language).

> VOL is currently in the early design stage, so its syntax may change.

# 1. Value

The basic unit of a VOL program is a Value.

```vol
10
"hello"
true
```

Functions, Records, and control flow are also Values.

---

# 2. Declaration

The basic declaration syntax in VOL is:

```vol
name: type = value
```

Example:

```vol
age: Int = 20
name: String = "Kim"
enabled: Bool = true
```

All declarations follow this form whenever possible.

---

# 3. Assignment

The value of an existing binding is changed as follows:

```vol
age = 21
```

In general:

```vol
b = a
```

assigns the Value of `a` to `b`.

Function parameter bindings follow the same Value-passing rules.

---

# 4. Record

A Record is also a type.

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}
```

There is no separate declaration syntax for Records.

The general declaration rule is used as-is:

```vol
name: type = value
```

Record fields must have a type and a default value.

---

# 5. Null State

VOL does not have a separate `null` Value.

Every Record must be able to represent a null state.

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}
```

The meaning of `null` is:

```text
null = 0    normal
null = 1    null
```

Example:

```vol
user.null = 1
user.name = "Kim"
user.age = 20
```

Even when `name` and `age` contain values, the Record is considered null if `null = 1`.

The null state takes precedence over all other fields.

---

# 6. Field Access

Record fields are accessed with `.`.

```vol
user.name
user.age
```

Field values can be changed as follows:

```vol
user.name = "Kim"
user.age = 20
```

`x.field = value` changes a field of the corresponding Record Value.

---

# 7. Function

A function is also a Value. The function type is `fn`.

```vol
add: fn = (a: Int, b: Int) {
    a + b
}
```

Functions use the general declaration form:

```vol
name: type = value
```

Function call:

```vol
result: Int = add(10, 20)
```

Function parameter bindings use the same rules as ordinary Value assignments.

---

# 8. If

A conditional is also a Value.

```vol
adult: if = (age >= 20) {
    print("adult")
}
```

The body is not executed when the Value is declared.

```vol
adult
```

When `adult` is evaluated, its condition is evaluated and its body runs if the condition is true.

An `if` Value can therefore be stored and passed around.

```vol
check: if = adult
```

---

# 9. Loop

A loop is also a Value.

Basic numeric iteration is written as follows:

```vol
counter: loop = (0, 10, 1) (i) {
    print(i)
}
```

Form:

```vol
name: loop = (start, end, step) (variable) {
    body
}
```

Each value has the following meaning:

- `start`: starting value
- `end`: ending value
- `step`: increment or decrement interval
- `variable`: binding that receives the current iteration value

A loop is not executed when it is declared.

```vol
counter
```

The loop runs when the Value is evaluated.

---

# 10. Break

`break` terminates the currently executing loop.

```vol
counter: loop = (0, 10, 1) (i) {
    stop: if = (i == 5) {
        break
    }

    stop
    print(i)
}

counter
```

The Value model and exact semantics of `break` are still being designed.

---

# 11. Evaluation

VOL distinguishes between **creating** and **evaluating** a Value.

Example:

```vol
check: if = (age >= 20) {
    print("adult")
}
```

This code creates a Value named `check`.

The conditional is not executed at this point.

```vol
check
```

When `check` is evaluated, its condition and body are executed.

The same principle applies to executable Values such as `loop`.

---

# 12. Type Error

A Value with a mismatched type cannot be created.

```vol
age: Int = "hello"
```

This code does not create a valid `age` Value and then raise an error; creation of the Value itself fails.

Type errors are detected before a Value is created whenever possible.

---

# 13. Basic Example

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}

user: User = {
    name: String = "Rang"
    age: Int = 20
    null: Int = 0
}

greet: fn = (user: User) {
    print("Hello, " + user.name)
}

adult: if = (user.age >= 20) {
    print("adult")
}

counter: loop = (0, 10, 1) (i) {
    print(i)
}

greet(user)
adult
counter
```

# Core Rule

VOL's syntax is described, whenever possible, by a single structure:

```vol
name: type = value
```

Program execution is described as the process of creating, connecting, and evaluating Values.
