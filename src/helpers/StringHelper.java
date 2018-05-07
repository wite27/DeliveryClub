package helpers;

public class StringHelper {
    public static boolean safeEquals(String str1, String str2)
    {
        if (str1 == null || str2 == null)
            return false;

        return str1.equals(str2);
    }
}
