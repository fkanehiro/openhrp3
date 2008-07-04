// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/** @file CollisionDetector/server/CollisionDetector_impl.h
 * Implementation of CollisionDetector_impl and CollisionDetectorFactory_impl
 */

#ifndef OPENHRP_COLISIONDETECTOR_IMPL_H_INCLUDED
#define OPENHRP_COLISIONDETECTOR_IMPL_H_INCLUDED

#include "ORBwrap.h"
#include "CollisionDetector.h"
#include "CdCache.h"
#include "CdScene.h"


namespace OpenHRP {

	/**
	 *
	 * CollisionDetector_impl class
	 * @version 0.2
	 * @date    2002/02/18
	 *
	 */
	class CollisionDetector_impl : virtual public POA_OpenHRP::CollisionDetector,
								   virtual public PortableServer::RefCountServantBase
	{

	private:

		/**
		 * ORB
		 */
		CORBA_ORB_var orb_;
        
		// cache of models
		CdCharCache* cache_;
        
		// scene
		CdScene* scene_;
        
	public:

		/**
		 * constructor
		 * @param   orb         reference to ORB
		 * @param   cache
		 */
		CollisionDetector_impl(CORBA_ORB_ptr orb, CdCharCache* cache);

		/**
		 * destructor
		 */
		~CollisionDetector_impl();

		/**
		 * object destruction
		 */
		virtual void destroy();


		//
		// IDL:CollisionDetector/addModel:1.0
		//
		virtual void addModel(const char* charName,
							  BodyInfo_ptr model);

		//
		// IDL:CollisionDetector/addCollisionPair:1.0
		//
		virtual void addCollisionPair(
									  const LinkPair& colPair,
									  CORBA::Boolean convexsize1,
									  CORBA::Boolean convexsize2
									  );


		//
		// IDL:CollisionDetector/removeCollisionPair:1.0
		//
		virtual void removeCollisionPair(const LinkPair& colPair);


		//
		// IDL:CollisionDetector/queryIntersectionForDefinedPairs:1.0
		//
		virtual CORBA::Boolean queryIntersectionForDefinedPairs(
																CORBA::Boolean checkAll,
																const CharacterPositionSequence& characterPositions,
																LinkPairSequence_out collidedPairs
																);


		//
		// IDL:CollisionDetector/queryIntersectionForGivenPairs:1.0
		//
		virtual CORBA::Boolean queryIntersectionForGivenPairs(CORBA::Boolean checkAll,
															  const LinkPairSequence& checkPairs,
															  const CharacterPositionSequence& characterPositions,
															  LinkPairSequence_out collidedPairs);

		//
		// IDL:CollisionDetector/queryContactDeterminationForDefinedPairs:1.0
		//
		virtual CORBA::Boolean queryContactDeterminationForDefinedPairs
		(const CharacterPositionSequence& characterPositions,
		 CollisionSequence_out collisions);

		//
		// IDL:CollisionDetector/queryContactDeterminationForGivenPairs:1.0
		//
		virtual CORBA::Boolean queryContactDeterminationForGivenPairs(const LinkPairSequence& checkPairs,
																	  const CharacterPositionSequence& characterPositions,
																	  CollisionSequence_out collisions);
		
		//
		// IDL:CollisionDetector/clearCache:1.0
		//
		virtual void clearCache(const char* url);

		//
		// IDL:CollisionDetector/clearAllCache:1.0
		//
		virtual void clearAllCache();

	private:

		void _contactDetermination(CdCheckPair* rPair, Collision& collision);

		void _setCharacterData(const CharacterPositionSequence& characterPositions);

		int _contactIntersection(CdCheckPair* rPair);

		vector<double> getTrianglesOfLink( int linkIndex, BodyInfo_ptr binfo );

		vector<CdJoint *> joints;
	};

	/**
	 *
	 * CollisionDetectorFactory class
	 *
	 * @version 0.1
	 * @date    2002/02/12
	 *
	 */
	class CollisionDetectorFactory_impl
		: virtual public POA_OpenHRP::CollisionDetectorFactory,
		  virtual public PortableServer::RefCountServantBase
	{

	private:
		/**
		 * ORB
		 */
		CORBA_ORB_var orb_;

		// cache of models
		CdCharCache* cache_;
        
	public:

		/**
		 * constructor
		 * @param   orb     reference to ORB
		 */
		CollisionDetectorFactory_impl(CORBA_ORB_ptr orb);

		/**
		 * destructor
		 */
		~CollisionDetectorFactory_impl();

		/**
		 * creation
		 * @return 
		 */
		CollisionDetector_ptr create();

		void shutdown();
	};

}

#endif
