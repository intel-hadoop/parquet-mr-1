package parquet.schema;

import java.util.concurrent.Callable;
import org.junit.Assert;
import org.junit.Test;
import parquet.schema.PrimitiveType.PrimitiveTypeName;

import static parquet.schema.PrimitiveType.PrimitiveTypeName.*;
import static parquet.schema.Type.Repetition.*;

public class TestTypeBuilders {
  @Test
  public void testPaperExample() {
    MessageType expected =
        new MessageType("Document",
            new PrimitiveType(REQUIRED, INT64, "DocId"),
            new GroupType(OPTIONAL, "Links",
                new PrimitiveType(REPEATED, INT64, "Backward"),
                new PrimitiveType(REPEATED, INT64, "Forward")),
            new GroupType(REPEATED, "Name",
                new GroupType(REPEATED, "Language",
                    new PrimitiveType(REQUIRED, BINARY, "Code"),
                    new PrimitiveType(REQUIRED, BINARY, "Country")),
                new PrimitiveType(OPTIONAL, BINARY, "Url")));
    MessageType builderType = Types.buildMessage()
        .required(INT64).named("DocId")
        .optionalGroup()
            .repeated(INT64).named("Backward")
            .repeated(INT64).named("Forward")
            .named("Links")
        .repeatedGroup()
            .repeatedGroup()
                .required(BINARY).named("Code")
                .required(BINARY).named("Country")
            .named("Language")
            .optional(BINARY).named("Url")
            .named("Name")
        .named("Document");
    Assert.assertEquals(expected, builderType);
  }

  @Test
  public void testDecimalAnnotationBinary() {
    MessageType expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, BINARY, 0, "aDecimal",
            OriginalType.DECIMAL, new OriginalTypeMeta(9, 2)));
    MessageType builderType = Types.buildMessage()
        .required(BINARY).as(OriginalType.DECIMAL).precision(9).scale(2)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);
  }

  @Test
  public void testDecimalAnnotationFixed() {
    MessageType expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, FIXED_LEN_BYTE_ARRAY, 4, "aDecimal",
            OriginalType.DECIMAL, new OriginalTypeMeta(9, 2)));
    MessageType builderType = Types.buildMessage()
        .required(FIXED_LEN_BYTE_ARRAY).length(4)
            .as(OriginalType.DECIMAL).precision(9).scale(2)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);
  }

  @Test
  public void testDecimalAnnotationLengthCheck() {
    // maximum precision for 4 bytes is 9
    assertThrows("should reject precision 10 with length 4",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.required(FIXED_LEN_BYTE_ARRAY).length(4)
                .as(OriginalType.DECIMAL).precision(10).scale(2)
                .named("aDecimal");
          }
        });
    // maximum precision for 8 bytes is 19
    assertThrows("should reject precision 20 with length 8",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.required(FIXED_LEN_BYTE_ARRAY).length(8)
                .as(OriginalType.DECIMAL).precision(20).scale(4)
                .named("aDecimal");
          }
        });
  }

  @Test
  public void testFixedWithLength() {
    PrimitiveType expected = new PrimitiveType(REQUIRED, FIXED_LEN_BYTE_ARRAY, 7, "fixed");
    PrimitiveType fixed = Types.required(FIXED_LEN_BYTE_ARRAY).length(7).named("fixed");
    Assert.assertEquals(expected, fixed);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testFixedWithoutLength() {
    Types.required(FIXED_LEN_BYTE_ARRAY).named("fixed");
  }

  @Test
  public void testUTF8Annotation() {
    PrimitiveType expected = new PrimitiveType(REQUIRED, BINARY, "string", OriginalType.UTF8);
    PrimitiveType string = Types.required(BINARY).as(OriginalType.UTF8).named("string");
    Assert.assertEquals(expected, string);
  }

  @Test
  public void testUTF8AnnotationRejectsNonBinary() {
    PrimitiveTypeName[] nonBinary = new PrimitiveTypeName[]{
      BOOLEAN, INT32, INT64, INT96, DOUBLE, FLOAT
    };
    for (final PrimitiveTypeName type : nonBinary) {
      assertThrows("Should reject non-binary type: " + type,
          IllegalStateException.class, new Callable<Type>() {
            @Override
            public Type call() throws Exception {
              return Types.required(type).as(OriginalType.UTF8).named("string");
            }
          });
    }
    assertThrows("Should reject non-binary type: FIXED_LEN_BYTE_ARRAY",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.required(FIXED_LEN_BYTE_ARRAY).length(1)
                .as(OriginalType.UTF8).named("string");
          }
        });
  }


  /**
   * A convenience method to avoid a large number of @Test(expected=...) tests
   * @param message A String message to describe this assertion
   * @param expected An Exception class that the Runnable should throw
   * @param callable A Callable that is expected to throw the exception
   */
  public static void assertThrows(
      String message, Class<? extends Exception> expected, Callable callable) {
    try {
      callable.call();
      Assert.fail("No exception was thrown (" + message + "), expected: " +
          expected.getName());
    } catch (Exception actual) {
      Assert.assertEquals(message, expected, actual.getClass());
    }
  }
}