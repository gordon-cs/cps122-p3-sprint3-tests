package library.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.time.LocalDate;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LibraryDatabaseTest {
  private TreeSet<String> callNumbers;
  private TreeSet<String> emails;
  private LibraryDatabase db;

  @BeforeEach
  public void setUp() {
    db = LibraryDatabase.getInstance(true);
    db.addBook("Title1", "Author1", "CallNumber1");
    db.addBook("Title2", "Author2", "CallNumber2");
    db.addBook("Title3", "Author3", "CallNumber3");
    callNumbers = new TreeSet<String>();
    callNumbers.add("CallNumber1");
    callNumbers.add("CallNumber2");
    callNumbers.add("CallNumber3");
    db.addBorrower("FirstName1", "LastName1", "Email1", "Phone1");
    db.addBorrower("FirstName2", "LastName2", "Email2", "Phone2");
    emails = new TreeSet<String>();
    emails.add("Email1");
    emails.add("Email2");
  }

  @Test
  public void testAddBook() {
    assertTrue(callNumbers.tailSet("").equals(db.getCallNumbers()));
  }

  @Test
  public void testAddBorrower() {
    assertTrue(emails.tailSet("").equals(db.getEmails()));
  }

  @Test
  public void testGetBookCsv() {
    String expectedCsv =
        "\"Title1\",\"Author1\",\"CallNumber1\"\n"
            + "\"Title2\",\"Author2\",\"CallNumber2\"\n"
            + "\"Title3\",\"Author3\",\"CallNumber3\"\n";
    assertEquals(expectedCsv, db.getBookCsv());
  }

  @Test
  public void testGetBorrowerCsv() {
    String expectedCsv =
        "\"FirstName1\",\"LastName1\",\"Email1\",\"Phone1\"\n"
            + "\"FirstName2\",\"LastName2\",\"Email2\",\"Phone2\"\n";
    assertEquals(expectedCsv, db.getBorrowerCsv());
  }

  // It's possible to write good tests for writeToFile() and readFromFile(), but it would
  // complicate your LibraryDatabase code. The problem is that de-serialization (in readFromFile())
  // creates a new instance of LibraryDatabase, which is a singleton.  But changing that would
  // require making the class more complicated to write.  So for now, simply trust that I have
  // provided working code for them, and be sure to test all the code you write.

  // Nevertheless, we can do a little testing of writeToFile().
  @Test
  public void testWriteToFile() {
    try {
      db.writeToFile("testLibraryOutput.db");
    } catch (Exception e) {
      fail("Exception thrown while writing to file: " + e.getMessage());
    }
    // Check if the file was created

    File file = new File("testLibraryOutput.db");
    assertTrue(file.exists(), "Output file should exist after writing.  ");
    // Leave the test file, in case it's useful for debugging
    // Checking the contents is complicated, because de-serializing creates a new object, but the
    // class is a singleton.  For now, we won't complicate the class to work around that.
  }

  @Test
  public void testCheckoutSuccess() {
    // Test successful checkout
    boolean result = db.checkout("CallNumber1", "Email1");
    assertTrue(result, "Checkout should succeed with valid book and borrower");

    // Verify the book is checked out
    assertTrue(db.isCheckedOut("CallNumber1"), "Book should be marked as checked out");

    // Verify due date is set (should be around 28 days from now)
    LocalDate dueDate = db.getDueDate("CallNumber1");
    assertNotNull(dueDate, "Due date should be set for checked out book");
    assertTrue(
        dueDate.isAfter(LocalDate.now().plusDays(27)),
        "Due date should be at least 28 days in future");
    assertTrue(
        dueDate.isBefore(LocalDate.now().plusDays(29)),
        "Due date should be at most 28 days in future");
  }

  @Test
  public void testCheckoutInvalidBook() {
    // Try to check out non-existent book
    boolean result = db.checkout("NonExistentBook", "Email1");
    assertFalse(result, "Checkout should fail with invalid book");
  }

  @Test
  public void testCheckoutInvalidBorrower() {
    // Try to check out to non-existent borrower
    boolean result = db.checkout("CallNumber1", "NonExistentEmail");
    assertFalse(result, "Checkout should fail with invalid borrower");
  }

  @Test
  public void testCheckoutAlreadyCheckedOut() {
    // First checkout should succeed
    boolean firstResult = db.checkout("CallNumber1", "Email1");
    assertTrue(firstResult, "First checkout should succeed");

    // Second checkout of same book should fail
    boolean secondResult = db.checkout("CallNumber1", "Email2");
    assertFalse(secondResult, "Checkout should fail when book is already checked out");
  }

  @Test
  public void testReturnBook() {
    // First checkout the book
    boolean checkoutResult = db.checkout("CallNumber1", "Email1");
    assertTrue(checkoutResult, "Checkout should succeed");

    // Return the book
    boolean returnResult = db.returnBook("CallNumber1");
    assertTrue(returnResult, "Return should succeed for checked out book");

    // Verify book is no longer checked out
    assertFalse(
        db.isCheckedOut("CallNumber1"), "Book should no longer be checked out after return");

    // Verify due date is no longer available
    assertNull(db.getDueDate("CallNumber1"), "Due date should be null for returned book");
  }

  @Test
  public void testReturnBookNotCheckedOut() {
    // Try to return a book that is not checked out
    boolean result = db.returnBook("CallNumber1");
    assertFalse(result, "Return should fail for book that is not checked out");
  }

  @Test
  public void testMultipleCheckoutsAndReturns() {
    // Check out multiple books
    assertTrue(db.checkout("CallNumber1", "Email1"), "First checkout should succeed");
    assertTrue(db.checkout("CallNumber2", "Email1"), "Second checkout should succeed");

    // Verify both are checked out
    assertTrue(db.isCheckedOut("CallNumber1"), "First book should be checked out");
    assertTrue(db.isCheckedOut("CallNumber2"), "Second book should be checked out");

    // Return first book
    assertTrue(db.returnBook("CallNumber1"), "Return of first book should succeed");

    // Verify one is returned, one is still checked out
    assertFalse(db.isCheckedOut("CallNumber1"), "First book should no longer be checked out");
    assertTrue(db.isCheckedOut("CallNumber2"), "Second book should still be checked out");

    // Should be able to check out the first book again
    assertTrue(db.checkout("CallNumber1", "Email2"), "Re-checkout of returned book should succeed");
    assertTrue(db.isCheckedOut("CallNumber1"), "First book should be checked out again");
  }

  @Test
  public void testGetDueDateForNonCheckedOutBook() {
    // Due date should be null for a book that is not checked out
    assertNull(
        db.getDueDate("CallNumber1"), "Due date should be null for book that is not checked out");
  }

  @Test
  public void testReturnSuccess() {
    // First checkout the book
    boolean checkedOut = db.checkout("CallNumber1", "Email1");
    assertTrue(checkedOut, "Checkout should succeed for available book");
    assertTrue(db.isCheckedOut("CallNumber1"), "Book should be checked out");

    // Return the book
    boolean returned = db.returnBook("CallNumber1");
    assertTrue(returned, "Return should succeed for checked out book");

    // Verify the book is no longer checked out
    assertFalse(db.isCheckedOut("CallNumber1"), "Book should no longer be checked out");
  }

  @Test
  public void testReturnNotCheckedOut() {
    // Try to return a book that's not checked out
    boolean returned = db.returnBook("CallNumber1");
    assertFalse(returned, "Return should fail for book that's not checked out");
  }

  @Test
  public void testRenewLoan() {
    // Checkout a book
    db.checkout("CallNumber1", "Email1");

    // Record the original due date
    var originalDueDate = db.getDueDate("CallNumber1");

    // Renew the loan
    assertTrue(db.renew("CallNumber1"), "Should be able to renew loan once");

    // Due date should be extended by 28 days
    assertEquals(
        originalDueDate.plusDays(28),
        db.getDueDate("CallNumber1"),
        "Due date should be extended by 28 days");

    // Should not be able to renew again
    assertFalse(db.renew("CallNumber1"), "Should not be able to renew loan twice");
  }
}
