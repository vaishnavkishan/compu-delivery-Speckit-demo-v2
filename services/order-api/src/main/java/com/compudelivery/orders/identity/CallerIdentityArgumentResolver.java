package com.compudelivery.orders.identity;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Injects the {@link CallerIdentity} resolved by {@link CallerIdentityFilter}
 * into controller methods.
 */
public class CallerIdentityArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return CallerIdentity.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Object attribute = webRequest.getAttribute(CallerIdentityFilter.REQUEST_ATTRIBUTE,
				NativeWebRequest.SCOPE_REQUEST);
		return attribute != null ? attribute : new CallerIdentity(null, null);
	}
}
