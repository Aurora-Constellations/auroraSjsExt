package org.aurora.sjsast.arnold
import org.scalatest._
import propspec._

class SetSpec extends AnyPropSpec {
  override def withFixture(test: NoArgTest) = { // Define a shared fixture
    // Shared setup (run at beginning of each test)
    try test()
    finally {
      // Shared cleanup (run at end of each test)
    }
  }
  // Define tests with 'property', a test name string in parentheses,
  // and test body in curly braces
  property("An empty Set should have size 0") {
    assert(Set.empty.size == 0)
  }
  // To ignore a test, change 'property' to 'ignore'
  ignore("Invoking head on an empty Set should produce NoSuchElementException") {
    intercept[NoSuchElementException] {
      Set.empty.head
    }
  }
  // Define a pending test by using (pending) for the body
  property("An empty Set's isEmpty method should return false") (pending)
  // Tag a test by placing a tag object after the test name
  import tagobjects.Slow
  property("An empty Set's nonEmpty method should return true", Slow) { 
    assert(!Set.empty.nonEmpty)
  }
}

// Can also pass fixtures into tests with FixtureAnyPropSpec
class StringSpec extends FixtureAnyPropSpec {
  type FixtureParam = String // Define the type of the passed fixture object
  override def withFixture(test: OneArgTest) = {
    // Shared setup (run before each test), including...
    val fixture = "a fixture object" // ...creating a fixture object
    try test(fixture) // Pass the fixture into the test
    finally {
      // Shared cleanup (run at end of each test)
    }
  }
  property("The passed fixture can be used in the test") { s => // Fixture passed in as s
    assert(s == "a fixture object")
  }
}

@DoNotDiscover // Disable discovery of a test class
class InvisibleSpec extends AnyPropSpec { /*code omitted*/ }

@Ignore // Ignore all tests in a test class
class IgnoredSpec extends AnyPropSpec { /*code omitted*/ }

import tags.Slow
@Slow // Mark all tests in a test class with a tag
class SlowSpec extends AnyPropSpec { /*code omitted*/ }
