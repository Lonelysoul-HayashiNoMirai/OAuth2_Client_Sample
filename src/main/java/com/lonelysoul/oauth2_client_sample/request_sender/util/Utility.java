package com.lonelysoul.oauth2_client_sample.request_sender.util;

import java.util.Collection;
import java.util.Map;

import static java.lang.reflect.Array.getLength;
import static org.springframework.util.CollectionUtils.isEmpty;

public final class Utility {

    private Utility (){}

    public static boolean isNullOrEmpty (Object blankableObject){

        if (blankableObject == null){
            return true;
        }

        if (blankableObject instanceof String){
            return ((String) blankableObject).isBlank ();
        }
        else if (blankableObject instanceof Collection<?>){
            return isEmpty ((Collection<?>) blankableObject);
        }
        else if (blankableObject.getClass ().isArray ()){
            return getLength (blankableObject) == 0;
        }
        else if (blankableObject instanceof Map<?, ?>){
            return ((Map<?, ?>) blankableObject).isEmpty ();
        }
        else {
            return false;
        }
    }
}
