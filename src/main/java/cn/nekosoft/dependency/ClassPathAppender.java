package cn.nekosoft.dependency;

import java.io.File;
import java.nio.file.Path;

public interface ClassPathAppender {
   void appendPath(Path path);

   void appendFile(File file);
}
