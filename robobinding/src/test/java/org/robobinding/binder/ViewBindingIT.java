package org.robobinding.binder;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robobinding.binder.PendingAttributesForViewBuilder.aPendingAttributesForView;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robobinding.BinderProvider;
import org.robobinding.BindingContext;
import org.robobinding.ItemBinder;
import org.robobinding.PendingAttributesForView;
import org.robobinding.annotation.ItemPresentationModel;
import org.robobinding.aspects.PresentationModel;
import org.robobinding.attribute.ChildAttributeResolverMappings;
import org.robobinding.attribute.ChildAttributeResolvers;
import org.robobinding.attribute.ResolvedGroupAttributes;
import org.robobinding.presentationmodel.PresentationModelAdapterFactory;
import org.robobinding.viewattribute.AttributeBindingException;
import org.robobinding.viewattribute.ViewBinding;
import org.robobinding.viewattribute.BindingAttributeMappings;
import org.robobinding.viewattribute.grouped.AbstractGroupedViewAttribute;
import org.robobinding.viewattribute.grouped.ChildViewAttributesBuilder;
import org.robobinding.viewattribute.property.PropertyViewAttribute;
import org.robobinding.widget.listview.SparseBooleanArrayUtils;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.xtremelabs.robolectric.RobolectricTestRunner;

/**
 *
 * @since 1.0
 * @version $Revision: 1.0 $
 * @author Robert Taylor
 */
@RunWith(RobolectricTestRunner.class)
public class ViewBindingIT {
    private BindingAttributeResolver bindingAttributeResolver;
    private BindingContext bindingContext;

    @Before
    public void setUp() {
	bindingAttributeResolver = new BindingAttributeResolverBuilder()
		.mapView(BuggyCustomView.class, new BuggyCustomViewAttributeMapper())
		.build();
	bindingContext = newBindingContext();
    }

    @Test
    public void whenBindingValidResolvedAttributes_thenShouldNotThrowException() {
	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForEditText().withAttribute("text",
		"${name}").build());

	ViewBindingErrors viewBindingErrors = resolvedBindingAttributes.bindTo(bindingContext);
	viewBindingErrors.assertNoErrors();
    }

    @Test(expected = ViewBindingErrors.class)
    public void whenBindingInvalidResolvedPropertyAttributes_thenThrowException() {
	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForEditText().withAttribute("text",
		"${nonExistentProperties}").build());

	ViewBindingErrors viewBindingErrors = resolvedBindingAttributes.bindTo(bindingContext);
	viewBindingErrors.assertNoErrors();
    }

    @Test(expected = ViewBindingErrors.class)
    public void whenBindingInvalidResolvedCommandAttributes_thenThrowException() {
	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForEditText().withAttribute(
		"onTextChanged", "setName").build());

	ViewBindingErrors viewBindingErrors = resolvedBindingAttributes.bindTo(bindingContext);
	viewBindingErrors.assertNoErrors();
    }

    @Test
    public void whenBindingMultipleInvalidResolvedAttributes_thenThrowExceptionReferringToEachOne() {
	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForEditText()
		.withAttribute("visibility", "{nonExistentProperty}").withAttribute("onTextChanged", "setName").build());

	try {
	    ViewBindingErrors viewBindingErrors = resolvedBindingAttributes.bindTo(bindingContext);
	    viewBindingErrors.assertNoErrors();
	    fail("Expected exception to be thrown");
	} catch (ViewBindingErrors e) {
	    assertHasAttributeError(e, "visibility");
	    assertHasAttributeError(e, "onTextChanged");
	    assertThat(e.numErrors(), is(2));
	}
    }

    @Test
    public void whenBindingMultipleInvalidResolvedGroupChildAttributes_thenThrowExceptionReferringToEachOne() {
	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForAdapterView()
		.withAttribute("source", "{nonExistentProperty}").withAttribute("itemLayout", "@layout/non_existent_layout").build());

	try {
	    ViewBindingErrors viewBindingErrors = resolvedBindingAttributes.bindTo(bindingContext);
	    viewBindingErrors.assertNoErrors();
	    fail("Expected exception to be thrown");
	} catch (ViewBindingErrors e) {
	    assertHasAttributeError(e, "source");
	    assertHasAttributeError(e, "itemLayout");
	    assertThat(e.numErrors(), is(2));
	}
    }

    @Test
    public void whenBindingMultipleInvalidResolvedSubViewAttributes_thenThrowExceptionReferringToFirstFailure() {
	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForAdapterView()
		.withAttribute("itemLayout", "{layout}").withAttribute("source", "{dataSource}")
		.withAttribute("footerLayout", "@layout/non_existent_layout").withAttribute("footerVisibility", "{nonExistentProperty}")
		.withAttribute("footerPresentationModel", "{nonExistentProperty}").build());

	try {
	    ViewBindingErrors viewBindingErrors = resolvedBindingAttributes.bindTo(bindingContext);
	    viewBindingErrors.assertNoErrors();
	    fail("Expected exception to be thrown");
	} catch (ViewBindingErrors e) {
	    assertHasAttributeError(e, "footerLayout");
	    assertThat(e.numErrors(), is(1));
	}
    }

    @Ignore@Test(expected = ProgrammingError.class)
    public void whenAnUnexpectedExceptionIsThrownDuringBinding_thenErrorShouldNotBeSuppressed() {
	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForBuggyCustomView().withAttribute(
		BuggyCustomView.BUGGY_PROPERTY_ATTRIBUTE, "{name}").build());

	resolvedBindingAttributes.bindTo(bindingContext);
    }

    @Test(expected = ProgrammingError.class)
    public void whenAnUnexpectedExceptionIsThrownDuringGroupChildAttributeBinding_thenErrorShouldNotBeSuppressed() {
	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForBuggyCustomView().withAttribute(
		BuggyCustomView.BUGGY_GROUP_CHILD_ATTRIBUTE, "{name}").build());

	resolvedBindingAttributes.bindTo(bindingContext);
    }

    @Test
    public void whenBindingWithPreInitializingViews_thenInitializedValueShouldNotBeErased() {
	ListView listView = new ListView(new Activity());
	listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

	ResolvedBindingAttributesForView resolvedBindingAttributes = resolveBindingAttributes(aPendingAttributesForView(listView)
		.withAttribute("checkedItemPositions", "{checkedItemPositions}").withAttribute("itemLayout", "{layout}")
		.withAttribute("source", "{dataSource}").build());

	ViewBindingErrors viewBindingErrors = resolvedBindingAttributes.bindTo(bindingContext);
	viewBindingErrors.assertNoErrors();
	resolvedBindingAttributes.preinitializeView(bindingContext);
	viewBindingErrors.assertNoErrors();

	assertThat(SparseBooleanArrayUtils.toSet(listView.getCheckedItemPositions()), equalTo((Set<Integer>) newHashSet(0, 2)));
    }

    private PendingAttributesForViewBuilder aPendingAttributesForEditText() {
	EditText editText = new EditText(new Activity());
	return aPendingAttributesForView(editText);
    }

    private PendingAttributesForViewBuilder aPendingAttributesForAdapterView() {
	AdapterView<?> adapterView = new ListView(new Activity());
	return aPendingAttributesForView(adapterView);
    }

    private PendingAttributesForViewBuilder aPendingAttributesForBuggyCustomView() {
	BuggyCustomView buggyCustomView = new BuggyCustomView(new Activity());
	return aPendingAttributesForView(buggyCustomView);
    }

    private ResolvedBindingAttributesForView resolveBindingAttributes(PendingAttributesForView pendingAttributesForView) {
	ViewResolutionResult resolutionResult = bindingAttributeResolver.resolve(pendingAttributesForView);
	resolutionResult.assertNoErrors();
	return resolutionResult.getResolvedBindingAttributes();
    }

    private void assertHasAttributeError(ViewBindingErrors e, String attribute) {
	Collection<AttributeBindingException> attributeErrors = e.getAttributeErrors();
	for (AttributeBindingException attributeError : attributeErrors) {
	    if (attributeError.getAttributeName().equals(attribute)) {
		return;
	    }
	}

	fail("Binding error for " + attribute + " was not reported");
    }

    @SuppressWarnings("unchecked")
    private BindingContext newBindingContext() {
	BinderProvider binderFactory = mock(BinderProvider.class);
	ItemBinder itemBinder = mock(ItemBinder.class);
	when(itemBinder.inflateAndBind(anyInt(), anyObject(), anyCollection())).then(new Answer<View>() {
	    @Override
	    public View answer(InvocationOnMock invocation) throws Throwable {
		return new View(new Activity());
	    }
	});
	when(binderFactory.getItemBinder()).thenReturn(itemBinder);
	return new BindingContext(
		binderFactory,
		new Activity(),
		new PresentationModelAdapterFactory().create(new PresentationModelForTest()),
		true);
    }

    @PresentationModel
    public static class PresentationModelForTest {
	private String name;

	public String getName() {
	    return name;
	}

	public void setName(String name) {
	    this.name = name;
	}

	@ItemPresentationModel(ItemPresentationModelForTest.class)
	public List<String> getDataSource() {
	    return newArrayList("string1", "string2", "string3");
	}

	public Set<Integer> getcheckedItemPositions() {
	    return newHashSet(0, 2);
	}

	public int getLayout() {
	    return R.layout.simple_list_item_1;
	}
    }

    public static class ItemPresentationModelForTest implements org.robobinding.itempresentationmodel.ItemPresentationModel<String> {
	@Override
	public void updateData(int index, String bean) {
	}
    }

    private static class BuggyCustomView extends View {

	static final String BUGGY_PROPERTY_ATTRIBUTE = "buggyPropertyAttribute";
	static final String BUGGY_GROUP_CHILD_ATTRIBUTE = "buggyGroupChildAttribute";

	public BuggyCustomView(Context context) {
	    super(context);
	}
    }

    private static class BuggyCustomViewAttributeMapper implements ViewBinding<BuggyCustomView> {
	@Override
	public void mapBindingAttributes(BindingAttributeMappings<BuggyCustomView> mappings) {
	    mappings.mapProperty(BuggyPropertyAttribute.class, BuggyCustomView.BUGGY_PROPERTY_ATTRIBUTE);
	    mappings.mapGroupedAttribute(BuggyGroupedAttribute.class, BuggyCustomView.BUGGY_GROUP_CHILD_ATTRIBUTE);
	}
    }

    public static class BuggyPropertyAttribute implements PropertyViewAttribute<BuggyCustomView, String> {
	@Override
	public void updateView(BuggyCustomView view, String newValue) {
	    throw new ProgrammingError();
	}
    }

    public static class BuggyGroupedAttribute extends AbstractGroupedViewAttribute<BuggyCustomView> {

	@Override
	public void mapChildAttributeResolvers(ChildAttributeResolverMappings resolverMappings) {
	    resolverMappings.map(ChildAttributeResolvers.propertyAttributeResolver(), BuggyCustomView.BUGGY_GROUP_CHILD_ATTRIBUTE);
	}

	@Override
	public void setupChildViewAttributes(BuggyCustomView view, ChildViewAttributesBuilder<BuggyCustomView> childViewAttributesBuilder,
		BindingContext bindingContext) {
	    throw new ProgrammingError();
	}

	@Override
	public void validateResolvedChildAttributes(ResolvedGroupAttributes groupAttributes) {
	}
    }

    @SuppressWarnings("serial")
    public static class ProgrammingError extends RuntimeException {
    }
}
