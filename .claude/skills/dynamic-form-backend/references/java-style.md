# Java style reference

Generic Java and clean-code guidance moved out of `SKILL.md` so it is not loaded on every skill invocation. Project-specific rules live in the skill itself and in `backend/CLAUDE.md`.

## Clean Code Principles (Uncle Bob)

- **Single Responsibility Principle:** Each class and method has one, and only one, reason to change. If a class or method is doing too many things, split it.
- **Open/Closed Principle:** Classes should be open for extension but closed for modification. Prefer adding new behaviour via new classes rather than modifying existing ones.
- **Keep Methods Small:** Methods should do one thing. If a method exceeds ~20 lines, consider breaking it into smaller, focused methods.
- **Meaningful Names:** Use clear, intention-revealing names for classes, methods, and variables. The name should explain the *why*, not just the *what*.
- **Don't Repeat Yourself (DRY):** Avoid duplicating logic. Extract shared behaviour into a utility method or service.
- **Comments Are a Last Resort:** Code should be self-documenting. If a comment is needed, consider refactoring the code to make the intent clearer instead. Javadoc is acceptable only for public API surface or genuinely complex logic.
- **Favor Polymorphism Over Conditionals:** When branching on type or state, prefer polymorphism (strategy, visitor, etc.) over long if/else or switch/case chains.
- **Immutability:** Prefer immutable objects, especially for DTOs and value objects. Use Lombok `@Value` for immutable DTOs.

## Effective Java (Joshua Bloch)

### Object Creation
- **Prefer static factory methods over constructors** (Item 1): They have names, can return cached instances, and can return subtypes. Use when the constructor intent isn't obvious.
- **Use builders for classes with many parameters** (Item 2): When a constructor or factory has more than 3-4 parameters, use the Builder pattern. Lombok `@Builder` is preferred.
- **Enforce the singleton property with a private constructor or enum** (Item 3): Use `@Component` / `@Service` for Spring-managed singletons; avoid manual singleton patterns.
- **Prefer dependency injection over hardwiring resources** (Item 5): Never create service dependencies inside a class with `new`; always inject them (constructor injection).

### Methods and APIs
- **Check parameters for validity** (Item 49): Validate method parameters at the start. Throw `IllegalArgumentException` for bad values, `NullPointerException` (or use `Objects.requireNonNull`) for nulls.
- **Return empty collections or optionals, not null** (Item 54/55): Never return `null` for a collection — return `Collections.emptyList()`. For optional single values, return `Optional<T>` rather than null.
- **Use `Optional` for optional return values** (Item 55): Return `Optional<T>` from methods that may produce no result. Never return `Optional` from a method that returns a collection.
- **Keep APIs minimal** (Item 57 spirit): Don't expose more than necessary. Prefer package-private or private over public.

### Classes and Interfaces
- **Favor composition over inheritance** (Item 18): Extend only where an is-a relationship is genuine. Prefer delegating to a field of the superclass type.
- **Design and document for inheritance or prohibit it** (Item 19): If a class is not designed for extension, mark it `final` or make its constructor private.
- **Prefer interfaces over abstract classes** (Item 20): Interfaces allow multiple implementations and retrofitting. Use abstract classes only when sharing implementation state.
- **Use interfaces only to define types** (Item 22): Don't use interfaces as constant holders (constant interface anti-pattern).
- **Prefer enums over int constants** (Item 34): Use `enum` for any fixed set of named values; add methods to enums rather than switch statements on them elsewhere.

### Generics and Collections
- **Don't use raw types** (Item 26): Always parameterize generic types (`List<String>`, not `List`).
- **Prefer generic methods** (Item 30): Type parameters on methods make APIs safer and cleaner than casting.
- **Use bounded wildcards to increase API flexibility** (Item 31): Producer `? extends T`, consumer `? super T` (PECS).

### Exceptions
- **Use exceptions only for exceptional conditions** (Item 69): Never use exceptions for normal control flow.
- **Use checked exceptions for recoverable conditions, unchecked for programming errors** (Item 70): `RuntimeException` subclasses for bugs; checked exceptions only when the caller can realistically recover.
- **Avoid unnecessary checked exceptions** (Item 71): Prefer unchecked exceptions in most cases; checked exceptions are a burden on callers.
- **Favor standard exceptions** (Item 72): Use `IllegalArgumentException`, `IllegalStateException`, `NullPointerException`, `UnsupportedOperationException`, `NoSuchElementException` before creating custom ones.
- **Document all exceptions thrown by each method** (Item 74): Use `@throws` in Javadoc for checked exceptions; document unchecked ones where useful.
- **Never swallow exceptions silently** (Item 77): At minimum, log the exception. An empty catch block is almost always wrong.

### General Programming
- **Minimize the scope of local variables** (Item 57): Declare variables where first used; prefer `final` where possible.
- **Prefer for-each loops over traditional for loops** (Item 58): Use enhanced for loops unless you need the index or need to remove elements.
- **Know and use the standard library** (Item 59): Prefer `java.util`, `java.util.concurrent`, and `java.util.stream` over hand-rolled equivalents.
- **Avoid `float` and `double` for exact answers** (Item 60): Use `BigDecimal` for monetary or precise decimal calculations.
- **Prefer primitive types to boxed primitives** (Item 61): Avoid `Integer`, `Long`, etc. in performance-sensitive paths; be aware of unboxing NPE risk.
- **Avoid strings where other types are more appropriate** (Item 62): Don't use `String` to represent numeric IDs, enums, or structured data.
- **Beware the performance of string concatenation** (Item 63): Use `StringBuilder` for building strings in loops.
- **Refer to objects by their interfaces** (Item 64): Declare variables and parameters as the interface type (`List<String> list = new ArrayList<>()`, not `ArrayList<String>`).
