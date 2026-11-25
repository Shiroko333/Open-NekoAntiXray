package sakuyaayane.nekoantixray.lib.adventure.adventure.key;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.intellij.lang.annotations.Pattern;
import org.intellij.lang.annotations.RegExp;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PARAMETER})
@Pattern("(?:(^(?!\\.\\.$)[a-z0-9_.-]+$:)?|:)[a-z0-9_\\-./]+")
public @interface KeyPattern {
   @RegExp
   String NAMESPACE_PATTERN = "^(?!\\.\\.$)[a-z0-9_.-]+$";
   @RegExp
   String VALUE_PATTERN = "[a-z0-9_\\-./]+";

   @Documented
   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PARAMETER})
   @Pattern("^(?!\\.\\.$)[a-z0-9_.-]+$")
   @interface Namespace {
   }

   @Documented
   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PARAMETER})
   @Pattern("[a-z0-9_\\-./]+")
   @interface Value {
   }
}
