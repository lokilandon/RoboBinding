/**
 * Copyright 2011 Cheng Wei, Robert Taylor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.robobinding.viewattribute.ratingbar;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robobinding.presentationmodel.PresentationModelAdapter;
import org.robobinding.viewattribute.MockFunction;

import android.app.Activity;
import android.content.Context;
import android.widget.RatingBar;

import com.xtremelabs.robolectric.RobolectricTestRunner;

/**
 *
 * @since 1.0
 * @version $Revision: 1.0 $
 * @author Robert Taylor
 */
@RunWith(RobolectricTestRunner.class)
@Ignore //Waiting for Robolectric to get updated
public class OnRatingBarChangeAttributeTest
{
	private RatingBar ratingBar;
	private Context context = new Activity();
	private MockFunction mockFunction;
	private PresentationModelAdapter mockPresentationModelAdapter;
	private final String commandName = "someCommand";
	
	@Before
	public void setUp()
	{
		ratingBar = new RatingBar(null);
		mockFunction = new MockFunction();
		mockPresentationModelAdapter = mock(PresentationModelAdapter.class);
		when(mockPresentationModelAdapter.findFunction(commandName, RatingBarEvent.class)).thenReturn(mockFunction);
	}
	
	@Test
	public void whenRatingBarChanges_ThenInvokeCommand()
	{
		OnRatingBarChangeAttribute onClickAttribute = new OnRatingBarChangeAttribute(ratingBar, commandName);
		onClickAttribute.bind(mockPresentationModelAdapter, context);
		
		ratingBar.setRating(2f);
	
		assertTrue(mockFunction.commandInvoked);
	}
	
	@Test
	public void whenRatingBarChanges_ThenInvokeCommandWithSeekBarEvent()
	{
		OnRatingBarChangeAttribute onClickAttribute = new OnRatingBarChangeAttribute(ratingBar, commandName);
		onClickAttribute.bind(mockPresentationModelAdapter, context);
		
		ratingBar.setRating(2f);

		assertThat(mockFunction.argsPassedToInvoke[0], instanceOf(RatingBarEvent.class));
		RatingBarEvent ratingBarEvent = (RatingBarEvent)mockFunction.argsPassedToInvoke[0];
		assertThat(ratingBarEvent.getRatingBar(), sameInstance(ratingBar));
		assertThat(ratingBarEvent.getRating(), is(2f));
		assertTrue(ratingBarEvent.isFromUser());
	}
}