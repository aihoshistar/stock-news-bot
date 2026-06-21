package com.example.stocknewsbot.web;

import jakarta.servlet.http.HttpServletRequest;

public class FragmentViewResolver {

    private static final String FRAGMENT_HEADER = "X-Requested-With";
    private static final String FRAGMENT_VALUE = "Fragment";

    private FragmentViewResolver() {}

    public static boolean isFragmentRequest(HttpServletRequest request) {
        return FRAGMENT_VALUE.equals(request.getHeader(FRAGMENT_HEADER));
    }

    /**
     *
     * @param request           현재 요청
     * @param contentTemplate   content fragment가 정의된 템플릿 파일명
     * @param fragmentName      th:fragment 이름
     * @return                  document 요청이면 "layout", AJAX 요청이면 "Template :: fragment"
     */
    public static String resolve(HttpServletRequest request,
                                 String contentTemplate,
                                 String fragmentName) {
        return isFragmentRequest(request)
                ? contentTemplate + " :: " + fragmentName
                : "layout";
    }
}
