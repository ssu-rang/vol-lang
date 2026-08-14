# VOL

**Value-Oriented Language**

VOL은 프로그램을 **Value의 생성, 연결, 평가**로 설명하는 것을 목표로 하는 프로그래밍 언어입니다.

VOL의 핵심 목표는 기능을 줄이는 것이 아니라, 프로그래밍 언어를 구성하는 개념의 수를 줄이는 것입니다.

## Philosophy

### Everything is a Value

VOL에서 가능한 한 모든 것은 Value입니다.

숫자, 문자열, Bool, Record뿐만 아니라 함수와 제어 흐름도 Value입니다.

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

`20`, `"VOL"`, 함수, Record, 조건문, 반복문은 모두 Value로 취급됩니다.

---

### One Declaration Form

VOL은 가능한 한 모든 선언을 하나의 형태로 표현합니다.

```vol
name: type = value
```

예를 들어:

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

변수, Record, 함수마다 별도의 선언 문법을 만드는 대신 같은 구조를 사용합니다.

---

### Control Flow is a Value

VOL에서는 제어 흐름도 특별한 문장이 아니라 Value입니다.

```vol
check: if = (age >= 20) {
    print("adult")
}
```

위 코드는 조건을 즉시 실행하지 않습니다.

`check`라는 `if` Value를 생성합니다.

```vol
check
```

Value가 평가되는 순간 조건이 평가되고 필요한 코드가 실행됩니다.

반복문 역시 동일합니다.

```vol
counter: loop = (0, 10, 1) (i) {
    print(i)
}

counter
```

---

### Explicit Types, Explicit Defaults

VOL의 값은 자료형과 기본값을 명시적으로 가집니다.

```vol
name: String = ""
age: Int = 0
enabled: Bool = false
```

Record 역시 모든 필드의 자료형과 기본값을 정의합니다.

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}
```

이를 통해 값이 존재하는지 알 수 없는 암묵적인 상태를 최소화합니다.

---

### No Built-in Null Value

VOL에는 별도의 `null` 값이 존재하지 않습니다.

대신 Record가 자신의 null 상태를 명시적으로 표현합니다.

```vol
User: record = {
    name: String = ""
    age: Int = 0
    null: Int = 0
}
```

`null = 0`은 정상 상태를, `null = 1`은 null 상태를 의미합니다.

Record가 null 상태일 때는 다른 필드에 값이 존재하더라도 null 상태가 우선합니다.

---

## Goal

VOL의 궁극적인 목표는 **적은 수의 개념으로 프로그램 전체를 설명할 수 있는 언어**를 만드는 것입니다.

새로운 기능이 필요할 때마다 새로운 문법과 개념을 추가하는 대신, 기존의 Value 모델로 표현할 수 있는지를 먼저 고려합니다.

그 결과 VOL은 배우기 쉽고, 코드를 읽을 때 알아야 하는 암묵적인 규칙이 적으며, 작은 언어 모델 위에서 다양한 프로그램을 표현할 수 있는 언어를 지향합니다.

VOL은 단순히 문법이 짧은 언어를 목표로 하지 않습니다.

**언어 자체를 이해하는 데 필요한 개념의 수가 적은 언어**를 목표로 합니다.

## Status

VOL은 현재 초기 설계 및 프로토타이핑 단계입니다.

문법과 의미론은 구현 과정에서 변경될 수 있습니다.
