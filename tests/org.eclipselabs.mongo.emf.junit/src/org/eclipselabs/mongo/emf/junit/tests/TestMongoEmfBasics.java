/*******************************************************************************
 * Copyright (c) 2011 Bryan Hunt.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bryan Hunt - initial API and implementation
 *******************************************************************************/

package org.eclipselabs.mongo.emf.junit.tests;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bson.types.ObjectId;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.BinaryResourceImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipselabs.emf.query.Result;
import org.eclipselabs.mongo.emf.junit.model.ETypes;
import org.eclipselabs.mongo.emf.junit.model.ModelFactory;
import org.eclipselabs.mongo.emf.junit.model.ModelPackage;
import org.eclipselabs.mongo.emf.junit.model.PrimaryObject;
import org.eclipselabs.mongo.emf.junit.model.TargetObject;
import org.eclipselabs.mongo.junit.MongoUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author bhunt
 * 
 */
public class TestMongoEmfBasics extends TestHarness
{
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test(expected = IOException.class)
	public void testShortURI() throws IOException
	{
		// Setup : Create a target object to save.

		TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
		targetObject.setSingleAttribute("junit");

		// Test : Save the target object with a short (invalid) URI

		saveObject(targetObject, createObjectURI(targetObject.eClass(), new ObjectId()).appendSegment(""), null);
	}

	@Test(expected = IOException.class)
	public void testLongURI() throws IOException
	{
		// Setup : Create a target object to save.

		TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
		targetObject.setSingleAttribute("junit");

		// Test : Save the target object with a long (invalid) URI

		saveObject(targetObject, createCollectionURI(targetObject.eClass()).trimSegments(1), null);
	}

	@Test
	public void testExists() throws IOException
	{
		// Setup : Create and save a target object.

		TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
		targetObject.setSingleAttribute("junit");
		saveObject(targetObject);

		// Verify : exists() on the URI converter should return true

		assertTrue(targetObject.eResource().getResourceSet().getURIConverter().exists(targetObject.eResource().getURI(), null));
	}

	@Test
	public void testLoadNonexistentResource()
	{
		// Setup : Create an empty resource set

		ResourceSet resourceSet = MongoUtil.createResourceSet();

		// Test : Get the resource from the database

		Resource resource = resourceSet.getResource(createObjectURI(ModelPackage.Literals.TARGET_OBJECT, new ObjectId()), true);

		// Verify : The resource must be empty

		assertThat(resource, is(notNullValue()));
		assertThat(resource.getContents().size(), is(0));
	}

	@Test
	public void testTypes() throws IOException
	{
		// Setup

		ETypes eTypes = ModelFactory.eINSTANCE.createETypes();
		eTypes.setEBigDecimal(new BigDecimal(1));
		eTypes.setEBigInteger(new BigInteger(2, new Random()));
		eTypes.setEBoolean(true);
		eTypes.setEByte((byte) 3);
		eTypes.setEByteArray(new byte[] { 1, 2 });
		eTypes.setEChar('j');
		eTypes.setEDate(new Date());
		eTypes.setEDouble(1.0);
		eTypes.setEFloat(1.0f);
		eTypes.setEInt(1);
		eTypes.setELong(1L);
		eTypes.setEShort((short) 1);
		eTypes.setEString("j");

		// Test

		saveObject(eTypes);

		// Verify

		assertThat(eTypes.eResource().getURI().segmentCount(), is(3));
		assertThat(eTypes.eResource().getURI().segment(2), is(notNullValue()));
		MongoUtil.checkObject(eTypes);
	}

	@Test
	public void testTargetObjectWithObjectId() throws IOException
	{
		// Setup : Create a target object and ObjectId for the ID

		ObjectId id = new ObjectId();

		TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
		targetObject.setSingleAttribute("junit");

		// Test : Store the object to MongoDB

		saveObject(targetObject, createObjectURI(targetObject.eClass(), id), null);

		// Verify : Check that the object was stored correctly, and that is has the ID we specified.

		TargetObject actual = MongoUtil.checkObject(targetObject);
		assertThat(MongoUtil.getID(actual), is(id.toString()));
	}

	@Test
	public void testTargetObjectWithArbitraryId() throws IOException
	{
		// Setup : Create a target object and arbitrary string for the ID

		String id = "ID";

		TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
		targetObject.setSingleAttribute("junit");

		// Test : Store the object to MongoDB

		saveObject(targetObject, createObjectURI(targetObject.eClass(), id), null);

		// Verify : Check that the object was stored correctly, and that is has the ID we specified.

		TargetObject actual = MongoUtil.checkObject(targetObject);
		assertThat(MongoUtil.getID(actual), is(id));
	}

	@Test
	public void testUpdateWithOptionGenerateId() throws IOException
	{
		// Setup : Create a target object and arbitrary string for the ID

		String id = "ID";

		TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
		targetObject.setSingleAttribute("junit");

		saveObject(targetObject, createObjectURI(targetObject.eClass(), id), null);

		// Test : Update the object and store it back to MongoDB

		targetObject.setSingleAttribute("updated");
		targetObject.eResource().save(null);

		// Verify : Check that the object was stored correctly, and that is has the ID we specified.

		TargetObject actual = MongoUtil.checkObject(targetObject);
		assertThat(MongoUtil.getID(actual), is(id));

	}

	@Test
	public void testDeleteTargetObject() throws IOException
	{
		// Setup : Create and store the target object

		TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
		targetObject.setSingleAttribute("junit");
		saveObject(targetObject);

		// Test : Delete the target object

		targetObject.eResource().delete(null);

		// Verify : The target object was deleted

		assertThat(getCollection(targetObject.eClass()).getCount(), is(0L));
	}

	@Test
	public void testBatchInsert() throws IOException
	{
		// Setup : Create several target objects to be stored in the database

		ResourceSet resourceSet = MongoUtil.createResourceSet();
		Resource resource = resourceSet.createResource(createCollectionURI(ModelPackage.Literals.TARGET_OBJECT));
		int numberTargets = 10;

		for (int i = 0; i < numberTargets; i++)
		{
			TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
			targetObject.setSingleAttribute("junit " + i);
			resource.getContents().add(targetObject);
		}

		// Test : Store the objects in the database

		resource.save(null);

		// Verify : The resouce should contain a Result with proxies to all of the stored objects

		assertThat(resource.getContents().size(), is(1));
		assertThat(resource.getContents().get(0), is(instanceOf(Result.class)));

		Result result = (Result) resource.getContents().get(0);
		assertThat(result.getValues().size(), is(numberTargets));

		for (int i = 0; i < numberTargets; i++)
		{
			assertThat(result.getValues().get(i), is(instanceOf(TargetObject.class)));
			TargetObject targetObject = (TargetObject) result.getValues().get(i);
			assertThat(targetObject.getSingleAttribute(), is("junit " + i));
		}
	}

	@Test
	public void testXMIRepresentation() throws IOException
	{
		ResourceSet resourceSet = MongoUtil.createResourceSet();

		TargetObject targetObject1 = ModelFactory.eINSTANCE.createTargetObject();
		targetObject1.setSingleAttribute("one");
		saveObject(resourceSet, targetObject1);

		TargetObject targetObject2 = ModelFactory.eINSTANCE.createTargetObject();
		targetObject2.setSingleAttribute("two");
		saveObject(resourceSet, targetObject2);

		PrimaryObject primaryObject = ModelFactory.eINSTANCE.createPrimaryObject();
		primaryObject.setName("junit");
		primaryObject.getMultipleContainmentReferenceProxies().add(targetObject1);
		primaryObject.getMultipleContainmentReferenceProxies().add(targetObject2);

		saveObject(resourceSet, primaryObject);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		primaryObject.eResource().save(out, null);

		ResourceSet testResourceSet = MongoUtil.createResourceSet();

		testResourceSet.getURIConverter().getURIMap().put(URI.createURI("../TargetObject/"), targetObject1.eResource().getURI().trimSegments(1).appendSegment(""));
		Resource libraryXMI = new XMIResourceFactoryImpl().createResource(URI.createURI(temporaryFolder.newFile("model.xmi").getAbsolutePath()));
		testResourceSet.getResources().add(libraryXMI);

		libraryXMI.load(new ByteArrayInputStream(out.toByteArray()), null);
		MongoUtil.checkObject(primaryObject, libraryXMI.getContents().get(0));
	}

	@Test
	public void testBinaryRepresentation() throws IOException
	{
		ResourceSet resourceSet = MongoUtil.createResourceSet();

		TargetObject targetObject1 = ModelFactory.eINSTANCE.createTargetObject();
		targetObject1.setSingleAttribute("one");
		saveObject(resourceSet, targetObject1);

		TargetObject targetObject2 = ModelFactory.eINSTANCE.createTargetObject();
		targetObject2.setSingleAttribute("two");
		saveObject(resourceSet, targetObject2);

		PrimaryObject primaryObject = ModelFactory.eINSTANCE.createPrimaryObject();
		primaryObject.setName("junit");
		primaryObject.getMultipleContainmentReferenceProxies().add(targetObject1);
		primaryObject.getMultipleContainmentReferenceProxies().add(targetObject2);

		saveObject(resourceSet, primaryObject);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Map<Object, Object> options = new HashMap<Object, Object>();
		options.put(XMLResource.OPTION_BINARY, Boolean.TRUE);
		primaryObject.eResource().save(out, options);

		{
			ResourceSet testResourceSet = MongoUtil.createResourceSet();

			testResourceSet.getURIConverter().getURIMap().put(URI.createURI("../TargetObject/"), targetObject1.eResource().getURI().trimSegments(1).appendSegment(""));

			Resource libraryBinary = new BinaryResourceImpl(URI.createURI(temporaryFolder.newFile("model.binary").getAbsolutePath()));
			testResourceSet.getResources().add(libraryBinary);

			libraryBinary.load(new ByteArrayInputStream(out.toByteArray()), null);
			MongoUtil.checkObject(primaryObject, libraryBinary.getContents().get(0));
		}
		{
			ResourceSet testResourceSet = MongoUtil.createResourceSet();

			testResourceSet.getURIConverter().getURIMap().put(URI.createURI("../TargetObject/"), targetObject1.eResource().getURI().trimSegments(1).appendSegment(""));

			Resource libraryXMI = new XMIResourceFactoryImpl().createResource(URI.createURI(temporaryFolder.newFile("model.mongo.binary").getAbsolutePath()));
			testResourceSet.getResources().add(libraryXMI);

			libraryXMI.load(new ByteArrayInputStream(out.toByteArray()), options);
			MongoUtil.checkObject(primaryObject, libraryXMI.getContents().get(0));
		}
	}

	@Test
	public void testXMLRepresentation() throws IOException
	{
		ResourceSet resourceSet = MongoUtil.createResourceSet();

		TargetObject targetObject1 = ModelFactory.eINSTANCE.createTargetObject();
		targetObject1.setSingleAttribute("one");
		saveObject(resourceSet, targetObject1);

		TargetObject targetObject2 = ModelFactory.eINSTANCE.createTargetObject();
		targetObject2.setSingleAttribute("two");
		saveObject(resourceSet, targetObject2);

		PrimaryObject primaryObject = ModelFactory.eINSTANCE.createPrimaryObject();
		primaryObject.setName("junit");
		primaryObject.getMultipleContainmentReferenceProxies().add(targetObject1);
		primaryObject.getMultipleContainmentReferenceProxies().add(targetObject2);

		saveObject(resourceSet, primaryObject);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Map<Object, Object> options = new HashMap<Object, Object>();
		options.put(XMIResource.OPTION_SUPPRESS_XMI, Boolean.TRUE);
		primaryObject.eResource().save(out, options);

		{
			ResourceSet testResourceSet = MongoUtil.createResourceSet();

			testResourceSet.getURIConverter().getURIMap().put(URI.createURI("../TargetObject/"), targetObject1.eResource().getURI().trimSegments(1).appendSegment(""));

			Resource libraryXML = new XMLResourceFactoryImpl().createResource(URI.createURI(temporaryFolder.newFile("model.xml").getAbsolutePath()));
			testResourceSet.getResources().add(libraryXML);

			libraryXML.load(new ByteArrayInputStream(out.toByteArray()), null);
			MongoUtil.checkObject(primaryObject, libraryXML.getContents().get(0));
		}
		{
			ResourceSet testResourceSet = MongoUtil.createResourceSet();

			testResourceSet.getURIConverter().getURIMap().put(URI.createURI("../TargetObject/"), targetObject1.eResource().getURI().trimSegments(1).appendSegment(""));

			Resource libraryXMI = new XMIResourceFactoryImpl().createResource(URI.createURI(temporaryFolder.newFile("model.mongo.xml").getAbsolutePath()));
			testResourceSet.getResources().add(libraryXMI);

			libraryXMI.load(new ByteArrayInputStream(out.toByteArray()), options);
			MongoUtil.checkObject(primaryObject, libraryXMI.getContents().get(0));
		}
	}

	@Test
	public void testExtrinsicIDs() throws IOException
	{
		// Setup : Create a target object and set its extrinsic ID

		ResourceSet resourceSet = MongoUtil.createResourceSet();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl()
		{
			@Override
			public Resource createResource(URI uri)
			{
				return new XMIResourceImpl(uri)
				{
					@Override
					protected boolean useUUIDs()
					{
						return true;
					}
				};
			}
		});

		TargetObject targetObject = ModelFactory.eINSTANCE.createTargetObject();
		targetObject.setSingleAttribute("junit");
		saveObject(resourceSet, targetObject);

		XMLResource resource = (XMLResource) targetObject.eResource();
		String authorID = resource.getID(targetObject);

		// Test : Reload the target object

		resource.unload();
		resource.load(null);

		// Verify : Check that the extrinsic ID was restored.

		EObject obj = resource.getContents().get(0);
		String id = ((XMLResource) resource).getID(obj);
		assertThat(id, equalTo(authorID));
	}
}
