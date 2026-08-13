# VOL

> **Value-Oriented Language**
> 모든 것을 `Value`로 바라보는 단순한 범용 프로그래밍 언어입니다.

VOL은 객체(Object)가 아닌 **값(Value)** 을 프로그램의 중심에 두는 프로그래밍 언어 프로젝트입니다.

객체지향을 단순히 제거하는 것이 목적이 아니라, 현대 프로그래밍 언어에서 서로 다른 개념으로 다뤄지는 여러 요소를 가능한 한 **Value라는 하나의 모델로 통합하는 것**을 목표로 합니다.

> **모든 것은 Value이며, 프로그램은 Value의 구성과 전달, 변환으로 이루어진다.**

## 핵심 철학

### 1. 모든 것은 Value다

숫자, 문자열, Boolean, Record, Collection, Function 등 프로그램에서 직접 다루는 모든 것은 Value입니다.

```text
hp = 100
name = "Hero"

double(x: Int) = x * 2
operation = double
```

`100`, `"Hero"`, `double` 모두 Value이며 이름에 binding할 수 있습니다.

---

### 2. 프로그램은 Value의 흐름이다

VOL은 프로그램을 객체들이 서로 메시지를 주고받는 구조로 바라보지 않습니다.

기본적인 프로그램의 흐름은 다음과 같습니다.

```text
Value 생성
    ↓
Value 전달
    ↓
Function
    ↓
Value 변환
    ↓
Value 저장
```

즉 프로그램의 가장 기본적인 형태는 다음과 같습니다.

```text
Value → Function → Value
```

---

### 3. 데이터와 동작은 독립적이다

데이터가 함수를 소유할 필요는 없습니다.

```text
is_adult(age: Int) = age >= 18

result = is_adult(user.age)
```

함수는 자신에게 실제로 필요한 Value만 전달받습니다.

따라서 VOL은 `class`, `method`, `this`, `constructor`, `inheritance`와 같은 객체지향 개념을 기본 구성요소로 요구하지 않습니다.

---

### 4. Record도 Value다

Record는 객체를 생성하기 위한 설계도가 아니라 여러 Value를 하나로 묶은 Value입니다.

```text
record User {
    name: String = ""
    age: Int = 0
}
```

모든 field는 Type과 기본 Value를 가져야 합니다.

별도의 생성자나 `new`는 존재하지 않습니다.

```text
user = User

user.name = "Kim"
user.age = 20
```

---

### 5. 모든 Record는 null 상태를 가진다

VOL에는 별도의 `null` Value가 존재하지 않습니다.

대신 모든 Record는 공통적으로 null 상태를 가집니다.

```text
user.null = true
```

null 상태에서도 Record의 field는 존재할 수 있지만, Record를 해석할 때는 null 상태가 우선됩니다.

```text
user.null = false
```

를 통해 다시 일반 상태로 변경할 수 있습니다.

---

### 6. Function도 Value다

Function은 특별한 선언 대상이 아니라 Value입니다.

따라서 별도의 `fn` 키워드를 사용하지 않습니다.

```text
double(x: Int) = x * 2

operation = double

result = operation(10)
```

Function 역시 다른 Value처럼 binding하고 전달할 수 있습니다.

---

### 7. 가능한 모든 것은 Expression이다

VOL은 Statement와 Expression을 불필요하게 구분하지 않는 것을 지향합니다.

조건문과 블록 역시 Value를 만들 수 있습니다.

```text
message = if user.age >= 18 {
    "adult"
} else {
    "minor"
}
```

블록에서는 마지막 Expression이 블록의 결과 Value가 됩니다.

```text
damage(attack: Int) {
    base = attack * 2
    bonus = 10

    base + bonus
}
```

---

## Value Semantics

일반적인 대입은 독립적인 Value binding을 만듭니다.

```text
a = User
b = a

b.age = 20

print(a.age) // 0
print(b.age) // 20
```

Record 역시 기본적으로 Value semantics를 따릅니다.

실제 구현에서는 Copy-on-Write, structural sharing 등의 최적화를 사용할 수 있지만 이러한 구현 방식이 프로그램에서 관찰되는 의미를 변경해서는 안 됩니다.

함수 parameter로 전달된 Value는 함수 내부에서 변경할 수 있으며 해당 변경은 caller에서도 관찰될 수 있습니다.

```text
birthday(user: User) {
    user.age = user.age + 1
}

user = User
birthday(user)

print(user.age) // 1
```

즉 VOL에서는 **대입에 의한 복사와 함수 parameter 전달을 구분합니다.**

---

## 설계 원칙

VOL은 단순히 키워드가 적거나 코드가 짧은 언어를 목표로 하지 않습니다.

목표는 **프로그래머가 이해해야 하는 근본적인 개념의 수를 줄이는 것**입니다.

새로운 기능을 설계할 때 다음 질문을 먼저 합니다.

> **"새로운 키워드가 필요한가?"가 아니라 "새로운 개념이 정말 필요한가?"**

기존의 Value, Function, Record 등의 조합으로 문제를 자연스럽게 해결할 수 있다면 새로운 개념을 추가하지 않습니다.

또한 편의를 위해 Value의 의미를 암묵적으로 변경하지 않습니다.

```text
10 + 20       // 30
"Hello" + "!" // "Hello!"

10 + "20"     // TypeError
```

필요한 변환은 명시적으로 표현합니다.

---

## 현재 상태

VOL은 현재 초기 설계 및 프로토타입 단계에 있습니다.

언어의 문법과 의미론은 아직 확정되지 않았으며 구현 과정에서 변경될 수 있습니다.

초기 목표는 작은 언어 구현을 통해 VOL의 핵심 철학을 실제로 검증하는 것입니다.

### v0.1 목표

* 기본 Value와 Type
* Binding
* Function
* Record
* Record null state
* 기본 연산
* 조건 Expression
* Block Expression
* 기본 입출력
* 정적 Type 검사
* JVM에서 실행 가능한 코드 생성

---

## 궁극적인 목표

VOL의 궁극적인 목표는 **적은 수의 근본 개념으로 프로그램을 표현할 수 있는 단순하고 직관적인 프로그래밍 언어**가 되는 것입니다.

VOL이 추구하는 단순함은 단순히 문법이나 키워드의 수를 줄이는 것이 아닙니다.

새로운 기능이 추가되더라도 가능한 한 기존의 `Value`, `Function`, `Record`와 같은 개념의 조합으로 설명할 수 있도록 하여, 프로그래머가 새 기능을 사용할 때마다 새로운 사고방식을 배울 필요가 없도록 하는 것을 목표로 합니다.

프로그래머가 수많은 예외 규칙과 서로 다른 mental model을 암기하는 대신, **몇 가지 일관된 원칙을 이해하는 것만으로 언어 전체의 동작을 자연스럽게 예측할 수 있어야 합니다.**

또한 문법은 프로그래머의 사고를 방해하지 않아야 합니다. 불필요한 선언과 반복적인 표현을 줄이고, 코드가 가능한 한 사람이 문제를 생각하는 순서 그대로 읽히도록 합니다.

VOL이 궁극적으로 추구하는 것은 **기능이 적은 언어가 아니라, 많은 기능을 적은 개념으로 이해할 수 있는 언어**입니다.

> **적게 배우고, 일관되게 이해하며, 자연스럽게 표현한다.**

## 한 문장으로

> **VOL은 프로그램을 객체의 집합이 아닌 Value의 구성과 변환으로 바라보는 프로그래밍 언어입니다.**

