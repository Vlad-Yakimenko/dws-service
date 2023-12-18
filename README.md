## Things I would consider to implement/handle
- Throwing an exception and handling it via Controller Advice instead of manually returning BadRequest 
if we are not able to transfer money due the insufficient money in an account.
- Improving tests: every test class uses its own @SpringBootTest annotation that does not allow correct test 
context caching between runs.
  When we have a lot of tests and not very powerful machines, it can affect tests execution time in times!
- Extracting template strings to a dedicated utility class that will contain all such strings
- Using ValueExtractor for the atomic reference instead of defining a new validator
- Strictly avoid using getters for services' collaborators just to have access to them in tests, 
we must use DI for that!