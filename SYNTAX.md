# VOL Syntax

이 문서는 VOL(Value-Oriented Language)의 문법과 기본 의미론을 정의합니다.

> VOL은 현재 초기 설계 단계이므로 문법은 변경될 수 있습니다.

# 1. Value

VOL에서 프로그램을 구성하는 기본 단위는 Value입니다.

```vol
10
"hello"
true
```

함수, Record, 제어 흐름 역시 Value입니다.

---

# 2. Declaration

VOL의 기본 선언 문법은 다음과 같습니다.

```vol
name: type = value
```

예:

```vol
age: Int = 20
name: String = "Kim"
enabled: Bool = true
```

모든 선언은 가능한 한 이 형태를 따릅니다.

---

# 3. Assignment

이미 존재하는 binding의 값은 다음과 같이 변경합니다.

```vol
age = 21
```

기본적으로:

```vol
b = a
```

는 `a`의 Value를 `b`에 대입합니다.

함수의 parameter binding 역시 동일한 Value 전달 규칙을 따릅니다.

---

# 4. Record

Record 역시 하나의 타입입니다.

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}
```

Record를 위한 별도의 선언 구문은 존재하지 않습니다.

일반적인 선언 규칙:

```vol
name: type = value
```

을 그대로 사용합니다.

Record의 필드는 자료형과 기본값을 가져야 합니다.

---

# 5. Null State

VOL에는 별도의 `null` Value가 존재하지 않습니다.

모든 Record는 null 상태를 표현할 수 있어야 합니다.

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}
```

`null`의 의미는 다음과 같습니다.

```text
null = 0    normal
null = 1    null
```

예:

```vol
user.null = 1
user.name = "Kim"
user.age = 20
```

`name`과 `age`에 값이 존재하더라도 `null = 1`이면 해당 Record는 null 상태로 취급합니다.

null 상태가 다른 필드보다 우선합니다.

---

# 6. Field Access

Record의 필드는 `.`으로 접근합니다.

```vol
user.name
user.age
```

필드의 값은 다음과 같이 변경할 수 있습니다.

```vol
user.name = "Kim"
user.age = 20
```

`x.field = value`는 해당 Record Value의 field를 변경합니다.

---

# 7. Function

함수도 Value입니다.

함수의 타입은 `fn`입니다.

```vol
add: fn = (a: Int, b: Int) {
    a + b
}
```

함수 역시 일반적인 선언 형식을 사용합니다.

```vol
name: type = value
```

함수 호출:

```vol
result: Int = add(10, 20)
```

함수 parameter binding은 일반적인 Value 대입과 동일한 규칙을 사용합니다.

---

# 8. If

조건문 역시 Value입니다.

```vol
adult: if = (age >= 20) {
    print("adult")
}
```

선언 시점에는 body가 실행되지 않습니다.

```vol
adult
```

`adult`가 평가되는 시점에 조건을 평가하고, 조건이 참이면 body를 실행합니다.

따라서 `if` Value는 저장하고 전달할 수 있습니다.

```vol
check: if = adult
```

---

# 9. Loop

반복문 역시 Value입니다.

기본적인 숫자 반복은 다음과 같습니다.

```vol
counter: loop = (0, 10, 1) (i) {
    print(i)
}
```

형식:

```vol
name: loop = (start, end, step) (variable) {
    body
}
```

각 값은 다음 의미를 가집니다.

- `start`: 시작 값
- `end`: 종료 값
- `step`: 증가 또는 감소 간격
- `variable`: 현재 반복 값을 받을 binding

반복문 역시 선언할 때 실행되지 않습니다.

```vol
counter
```

Value가 평가되는 시점에 반복이 실행됩니다.

---

# 10. Break

`break`는 현재 실행 중인 반복을 종료합니다.

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

`break`의 Value 모델과 정확한 의미론은 아직 설계 중입니다.

---

# 11. Evaluation

VOL에서는 Value의 **생성**과 **평가**를 구분합니다.

예:

```vol
check: if = (age >= 20) {
    print("adult")
}
```

이 코드는 `check`라는 Value를 생성합니다.

이 시점에는 조건문이 실행되지 않습니다.

```vol
check
```

`check`가 평가되면 조건과 body가 실행됩니다.

동일한 원칙이 `loop`와 같은 실행 가능한 Value에도 적용됩니다.

---

# 12. Type Error

타입이 맞지 않는 Value는 생성될 수 없습니다.

```vol
age: Int = "hello"
```

위 코드는 유효한 `age` Value를 만든 뒤 오류를 발생시키는 것이 아니라, Value 생성 자체가 실패합니다.

타입 오류는 가능한 한 Value가 만들어지기 전에 검출합니다.

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

VOL의 문법은 가능한 한 다음 하나의 구조로 설명됩니다.

```vol
name: type = value
```

그리고 프로그램의 실행은 Value를 생성하고, 연결하고, 평가하는 과정으로 설명됩니다.
