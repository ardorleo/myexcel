/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.html2excel.utils;

import java.util.Objects;

/**
 * @author liaochong
 * @version 1.0
 */
public final class StringUtils {

    public static String toUpperCaseFirst(String content) {
        if (Objects.isNull(content) || content.isEmpty()) {
            return content;
        }
        if (content.length() == 1) {
            return content.toUpperCase();
        }
        String charAtFirst = content.substring(0, 1);
        return charAtFirst.toUpperCase() + content.substring(1);
    }

}
