/*******************************************************************************
 * Copyright (c) 2010 Bryan Hunt & Ed Merks.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bryan Hunt & Ed Merks - initial API and implementation
 *******************************************************************************/

package org.eclipselabs.mongo.emf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Map;

import org.bson.types.ObjectId;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.resource.impl.BinaryResourceImpl;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;
import org.eclipselabs.mongo.IMongoDB;
import org.eclipselabs.mongo.internal.emf.Activator;
import org.eclipselabs.mongo.internal.emf.MongoDBInputStream;
import org.eclipselabs.mongo.internal.emf.MongoDBOutputStream;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoURI;

/**
 * This EMF URI handler interfaces to MongoDB. This URI handler can handle URIs with the "mongo"
 * scheme. The URI path must have exactly 3 segments. The URI path must be of the form
 * /database/collection/{id} where id is optional the first time the EMF object is saved. When
 * building queries, do not specify an id, but make sure path has 3 segments by placing a "/" after
 * the collection.
 * 
 * Note that if the id is not specified when the object is first saved, MongoDB will assign the id
 * and the URI of the EMF Resource will be modified to include the id in the URI. Examples of valid
 * URIs:
 * 
 * mongo://localhost/data/people/
 * mongo://localhost/data/people/4d0a3e259095b5b334a59df0
 * 
 * @author bhunt
 * 
 */
public class MongoDBURIHandlerImpl extends URIHandlerImpl
{
	/**
	 * This constructor can be used in an OSGi environment and will get the IMongoDB service from the
	 * bundle activator.
	 */
	public MongoDBURIHandlerImpl()
	{
		this(Activator.getInstance().getMongoDB());
	}

	/**
	 * This constructor can be used in a standalone EMF environment. The user must supply an instance
	 * of IMongoDB.
	 * 
	 * @param mongoDB the MongoDB service
	 */
	public MongoDBURIHandlerImpl(IMongoDB mongoDB)
	{
		this.mongoDB = mongoDB;
	}

	@Override
	public boolean canHandle(URI uri)
	{
		// This handler should only accept URIs with the scheme "mongo"

		return "mongo".equalsIgnoreCase(uri.scheme());
	}

	@Override
	public void delete(URI uri, Map<?, ?> options) throws IOException
	{
		// It is assumed that delete is called with the URI path /database/collection/id

		DBCollection collection = getCollection(mongoDB, uri);
		collection.findAndRemove(new BasicDBObject(ID_KEY, getID(uri)));
	}

	@Override
	public OutputStream createOutputStream(final URI uri, final Map<?, ?> options) throws IOException
	{
		// This function may be called with a URI path with or without an id. If an id is not specified
		// the EMF resource URI will be modified to include the id generated by MongoDB.

		return new MongoDBOutputStream(mongoDB, uri, options, getResponse(options));
	}

	@Override
	public InputStream createInputStream(final URI uri, final Map<?, ?> options) throws IOException
	{
		return new MongoDBInputStream(mongoDB, uri, options, getResponse(options));
	}

	@Override
	public boolean exists(URI uri, Map<?, ?> options)
	{
		if (uri.query() != null)
			return false;

		try
		{
			DBCollection collection = getCollection(mongoDB, uri);
			return collection.findOne(new BasicDBObject(ID_KEY, getID(uri))) != null;
		}
		catch (Exception exception)
		{
			return false;
		}
	}

	public static DBCollection getCollection(IMongoDB mongoDB, URI uri) throws UnknownHostException, IOException
	{
		// We assume that the URI path has the form /database/collection/{id} making the
		// collection segment # 1.

		if (uri.segmentCount() != 3)
			throw new IOException("The URI is not of the form 'mongo:/database/collection/{id}");

		if (mongoDB == null)
			throw new IOException("MongoDB service is unavailable");

		String port = uri.port();
		MongoURI mongoURI = new MongoURI("mongodb://" + uri.host() + (port != null ? ":" + port : ""));
		return mongoDB.getMongo(mongoURI).getDB(uri.segment(0)).getCollection(uri.segment(1));
	}

	public static Object getID(URI uri) throws IOException
	{
		// Require that the URI path has the form /database/collection/{id} making the id segment # 2.

		if (uri.segmentCount() != 3)
			throw new IOException("The URI is not of the form 'mongo:/database/collection/{id}");

		String id = uri.segment(2);

		// If the ID was specified in the URI, we first attempt to create a MongoDB ObjectId. If
		// that fails, we assume that the client has specified a non ObjectId and return the raw data.

		try
		{
			return id.isEmpty() ? null : new ObjectId(id);
		}
		catch (Throwable t)
		{
			return id;
		}
	}

	public static boolean isNativeType(EDataType dataType)
	{
		String instanceClassName = dataType.getInstanceClassName();
		//@formatter:off
		return
			instanceClassName == "java.lang.String"  ||
			instanceClassName == "int"               ||
			instanceClassName == "boolean"           ||
			instanceClassName == "float"             ||
			instanceClassName == "long"              ||
			instanceClassName == "double"            ||
			instanceClassName == "java.util.Date"    ||
			instanceClassName == "short"             ||
			instanceClassName == "byte[]"            ||
			instanceClassName == "byte"              ||
			instanceClassName == "java.lang.Integer" ||
			instanceClassName == "java.lang.Boolean" ||
			instanceClassName == "java.lang.Long"    ||
			instanceClassName == "java.lang.Float"   ||
			instanceClassName == "java.lang.Double"  ||
			instanceClassName == "java.lang.Short"   ||
			instanceClassName == "java.lang.Byte";
		//@formatter:on
	}

	public static final String TIME_STAMP_KEY = "_timeStamp";
	public static final String ID_KEY = "_id";
	public static final String ECLASS_KEY = "_eClass";
	public static final String PROXY_KEY = "_eProxyURI";
	public static final String EXTRINSIC_ID_KEY = "_eId";
	public static final String OPTION_PROXY_ATTRIBUTES = BinaryResourceImpl.OPTION_STYLE_PROXY_ATTRIBUTES;
	public static final String OPTION_SERIALIZE_DEFAULT_ATTRIBUTE_VALUES = "SERIALIZE_DEFAULT";
	public static final String OPTION_USE_ID_ATTRIBUTE_AS_PRIMARY_KEY = "USE_ID_ATTRIBUTE_AS_PRIMARY_KEY";

	private IMongoDB mongoDB;
}
