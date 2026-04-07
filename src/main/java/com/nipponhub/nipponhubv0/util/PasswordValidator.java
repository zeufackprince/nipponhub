package com.nipponhub.nipponhubv0.util;

import java.util.regex.Pattern;

public class PasswordValidator {
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])"      // At least one lowercase letter
        + "(?=.*[A-Z])"      // At least one uppercase letter
        + "(?=.*\\d)"        // At least one digit
        + "(?=.*[@$!%*?&])"  // At least one special character
        + ".{8,}$"          // At least 8 characters
    );
    
    public static boolean isValid(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public static String getErrorMessage() {
        return "Password must be at least 8 characters with uppercase, lowercase, number, and special character (@$!%*?&)";
    }
}
