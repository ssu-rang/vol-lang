# VOL Syntax

> **Version 0.1 Draft**
> 이 문서는 현재까지 정의된 VOL의 기본 문법과 의미론을 설명합니다.
> 아직 초기 설계 단계이므로 구현 과정에서 변경될 수 있습니다.

---

## 1. Value

VOL에서 프로그램이 직접 다루는 모든 것은 **Value**입니다.

기본적인 Value는 다음과 같이 표현합니다.

```text
100
-10
3.14
"Hello"
true
false
```

Value에는 Type이 존재합니다.

```text
100       // Int
3.14      // Float
"Hello"   // String
true      // Bool
```

서로 호환되지 않는 Type을 언어가 임의로 변환하지 않습니다.

```text
10 + 20          // 30
"Hello" + "!"    // "Hello!"

10 + "20"        // TypeError
```

필요한 Type 변환은 명시적으로 수행합니다.

---

## 2. Binding

`=`을 사용해 Value에 이름을 binding합니다.

```text
hp = 100
name = "Hero"
alive = true
```

기존 binding의 Value를 사용할 수도 있습니다.

```text
a = 10
b = a
```

`b = a`는 `a`의 Value를 `b`에 독립적으로 binding합니다.

따라서 Record와 같은 mutable Value에서도:

```text
a = User
b = a

b.age = 20
```

`a`는 변경되지 않습니다.

```text
a.age    // 0
b.age    // 20
```

이것은 언어 수준의 **Value Semantics**입니다.

구현체는 이를 효율적으로 구현하기 위해 Copy-on-Write 등의 최적화를 사용할 수 있습니다.

---

## 3. Function

Function은 다음과 같이 정의합니다.

```text
double(x: Int) = x * 2
```

`fn` 등의 별도 함수 선언 키워드는 사용하지 않습니다.

여러 Expression이 필요한 경우 block을 사용합니다.

```text
damage(attack: Int) {
    base = attack * 2
    bonus = 10

    base + bonus
}
```

block의 마지막 Expression이 Function의 결과 Value가 됩니다.

따라서 일반적인 경우 별도의 `return`이 필요하지 않습니다.

---

## 4. Function은 Value다

Function 역시 Value이므로 다른 이름에 binding할 수 있습니다.

```text
double(x: Int) = x * 2

operation = double

result = operation(10)
```

Function을 다른 Function에 전달할 수도 있습니다.

```text
apply(value: Int, operation: Function) =
    operation(value)

result = apply(10, double)
```

모든 Function Value의 기본 Type은 `Function`입니다.

각 Function의 parameter와 결과 Type은 별도의 **signature**를 가집니다.

예:

```text
double(x: Int) = x * 2
```

의 signature는 개념적으로:

```text
(Int) -> Int
```

입니다.

---

## 5. Function Parameter

Function parameter에는 Type을 명시합니다.

```text
add(a: Int, b: Int) = a + b
```

Function parameter binding은 일반적인 `=` binding과 의미가 다릅니다.

```text
b = a
```

는 독립적인 Value를 만들지만:

```text
change(a)
```

는 Function이 caller의 Value를 직접 다룰 수 있습니다.

따라서 mutable Value를 Function 내부에서 변경하면 caller에서도 변경을 관찰할 수 있습니다.

```text
birthday(user: User) {
    user.age = user.age + 1
}

user = User
birthday(user)

user.age    // 1
```

---

## 6. Record

여러 Value를 하나의 구조로 묶으려면 `record`를 사용합니다.

```text
record User {
    name: String = ""
    age: Int = 0
}
```

Record는 Class가 아니라 **Value**입니다.

모든 field는 반드시 다음 두 가지를 가져야 합니다.

```text
Type
초기 Value
```

따라서:

```text
record User {
    name: String
}
```

과 같이 초기 Value가 없는 field는 허용되지 않습니다.

---

## 7. Record 생성

VOL에는 `new`나 constructor가 없습니다.

Record 자체가 이미 완전한 Value이므로 일반적인 binding을 사용합니다.

```text
user = User
```

이것은 `User` Value를 독립적으로 `user`에 binding합니다.

초기 상태:

```text
user.name    // ""
user.age     // 0
```

---

## 8. Field Access

`.`을 사용해 Record의 field에 접근합니다.

```text
user.name
user.age
```

field의 Value를 변경할 수도 있습니다.

```text
user.name = "Kim"
user.age = 20
```

`user.age = 20`은 `user` binding을 교체하는 것이 아니라 **user가 나타내는 Record Value 자체의 mutation**입니다.

---

## 9. Record Null State

VOL에는 별도의 `null` Value가 존재하지 않습니다.

대신 **모든 Record는 null state를 가집니다.**

Record 정의에 `null`을 직접 선언할 필요는 없습니다.

```text
record User {
    name: String = ""
    age: Int = 0
}
```

모든 `User` Value에는 자동으로:

```text
null: Bool = false
```

상태가 존재합니다.

null state는 직접 읽고 변경할 수 있습니다.

```text
user.null = true
```

null state를 해제하려면:

```text
user.null = false
```

를 사용합니다.

Record가 null state여도 field의 실제 Value는 유지될 수 있습니다.

```text
user.name = "Kim"
user.age = 20
user.null = true
```

이 상태는 유효합니다.

단, Record를 해석할 때는 **null state가 다른 field보다 우선합니다.**

즉 VOL의 null은:

> Value의 부재가 아니라 Record Value가 가지는 상태입니다.

---

## 10. Mutation

VOL의 Value는 필요에 따라 변경될 수 있습니다.

```text
user.age = 20
```

이는 Record Value의 mutation입니다.

Function parameter를 통해서도 mutation할 수 있습니다.

```text
birthday(user: User) {
    user.age = user.age + 1
}
```

```text
user = User

birthday(user)

user.age    // 1
```

따라서 VOL의 Function은 반드시 pure하지 않습니다.

---

## 11. If Expression

`if`는 Statement가 아니라 Expression입니다.

따라서 결과 Value를 직접 binding할 수 있습니다.

```text
message = if hp > 0 {
    "alive"
} else {
    "dead"
}
```

각 branch의 마지막 Expression이 해당 branch의 결과가 됩니다.

여러 조건도 사용할 수 있습니다.

```text
message = if hp > 50 {
    "healthy"
} else if hp > 20 {
    "injured"
} else {
    "danger"
}
```

---

## 12. Block Expression

Block 자체도 Value를 생성할 수 있습니다.

```text
damage = {
    base = attack * 2
    bonus = 10

    base + bonus
}
```

마지막 Expression:

```text
base + bonus
```

의 결과가 전체 block의 결과입니다.

같은 규칙이 Function body에도 적용됩니다.

---

## 13. 기본 연산

기본적인 산술 연산:

```text
a + b
a - b
a * b
a / b
```

비교 연산:

```text
a == b
a != b

a > b
a < b
a >= b
a <= b
```

Boolean 연산의 정확한 문법은 아직 확정하지 않았습니다.

---

## 14. Type Safety

VOL은 Value의 Type을 암묵적으로 변경하지 않습니다.

```text
10 + "20"
```

은 오류입니다.

```text
TypeError
```

컴파일 시점에 확인할 수 있는 Type 오류는 Value가 생성되기 전에 거부합니다.

명시적인 Type 변환 문법은 추후 정의합니다.

---

## 15. 기본 출력

프로토타입에서는 `print`를 기본 Function으로 사용합니다.

```text
print("Hello, VOL!")
```

다른 Function과 동일하게 Value를 전달합니다.

```text
message = "Hello"
print(message)
```

---

# 종합 예제

```text
record User {
    name: String = ""
    age: Int = 0
}

is_adult(age: Int) = age >= 18

birthday(user: User) {
    user.age = user.age + 1
}

status(user: User) {
    if user.null {
        "no user"
    } else if is_adult(user.age) {
        "adult"
    } else {
        "minor"
    }
}

user = User

user.name = "Kim"
user.age = 17

print(user.name)
print(status(user))

birthday(user)

print(user.age)
print(status(user))
```

개념적인 출력:

```text
Kim
minor
18
adult
```

---

# 아직 정의하지 않은 문법

다음 기능들은 현재 VOL v0.1 문법으로 확정하지 않았습니다.

* 반복문
* Collection
* Generic
* 사용자 정의 Variant/Sum Type
* Error handling
* 명시적 Type conversion 문법
* 모듈 및 import
* 접근 제어
* 비동기 처리
* 동시성
* 저수준 메모리 접근

이 기능들은 필요성이 확인된 이후 **기존 VOL의 개념으로 표현할 수 있는지를 먼저 검토한 뒤** 추가합니다.

---

# 문법 요약

```text
// Binding
x = 10

// Function
double(x: Int) = x * 2

// Function Value
operation = double

// Record
record User {
    name: String = ""
    age: Int = 0
}

// Record binding
user = User

// Mutation
user.age = 20

// Null state
user.null = true
user.null = false

// If Expression
message = if user.age >= 18 {
    "adult"
} else {
    "minor"
}

// Block Expression
result = {
    a = 10
    b = 20

    a + b
}
```

이것이 현재 정의된 **VOL v0.1 문법의 핵심 전부**입니다.
