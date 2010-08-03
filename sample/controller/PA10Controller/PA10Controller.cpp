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
/*!
 * @file  PA10Controller.cpp
 * @brief Sample PD component
 * $Date$
 *
 * $Id$
 */

#include "PA10Controller.h"

#include <iostream>

#define TIMESTEP 0.001

#define ANGLE_FILE "etc/angle.dat"
#define VEL_FILE   "etc/vel.dat"
#define GAIN_FILE  "etc/PDgain.dat"
#define CMP_FILE   "etc/compliance.dat"
#define RESULT_FILE "/home/harada/resultPA10_"


// Module specification
// <rtc-template block="module_spec">
static const char* PA10Controller_spec[] =
  {
    "implementation_id", "PA10Controller",
    "type_name",         "PA10Controller",
    "description",       "PA10Controller component",
    "version",           "0.1",
    "vendor",            "AIST",
    "category",          "Generic",
    "activity_type",     "DataFlowComponent",
    "max_instance",      "10",
    "language",          "C++",
    "lang_type",         "compile",
    // Configuration variables

    ""
  };
// </rtc-template>

PA10Controller::PA10Controller(RTC::Manager* manager)
  : RTC::DataFlowComponentBase(manager),
    // <rtc-template block="initializer">
    m_angleIn("angle", m_angle),
    m_torqueOut("torque", m_torque),
    
    m_wristForceIn("force_out",m_wristForce),

    // </rtc-template>
    dummy(0),
    qold(DOF)
{
  // Registration: InPort/OutPort/Service
  // <rtc-template block="registration">
  // Set service provider to Ports
  
  // Set service consumers to Ports
  
  // Set CORBA Service Ports
  
  // </rtc-template>

  string modelfile = "file:///usr/local/share/OpenHRP-3.1/sample/model/PA10/pa10.camera.wrl";

  char *argv[2];
  argv[0] = "";
  argv[1] = "-ORBInitRef NameService=corbaloc:iiop:localhost:2809/NameService";
  int argc = 2;

  co = new Body();
  loadBodyFromModelLoader(co, modelfile.c_str(), argc, argv);

}

PA10Controller::~PA10Controller()
{
  closeFiles();
  delete [] Pgain;
  delete [] Dgain;
}

RTC::ReturnCode_t PA10Controller::onInitialize()
{
  // <rtc-template block="bind_config">
  // Bind variables and configuration variable

  // Set InPort buffers
  addInPort("angle", m_angleIn);
  
  addInPort("force_out", m_wristForceIn);

  // Set OutPort buffer
  addOutPort("torque", m_torqueOut);
  
  // </rtc-template>

  Pgain = new double[DOF];
  Dgain = new double[DOF];

  gain.open(GAIN_FILE);
  if (gain.is_open()){
    for (int i=0; i<DOF; i++){
      gain >> Pgain[i];
      gain >> Dgain[i];
    }
    gain.close();

	Gp = dzeromatrix(7,7);
	Gd = dzeromatrix(7,7);

	for(int i=0; i<7; i++){
		Gp(i,i) = Pgain[i];
		Gd(i,i) = Dgain[i];
	}
  }else{
    std::cerr << GAIN_FILE << " not opened" << std::endl;
  }

  cmp.open(CMP_FILE);
  if (cmp.is_open()){
    Kp=dzeromatrix(6,6);
	Kd=dzeromatrix(6,6);
    for (int i=0; i<6; i++)		cmp >> Kp(i, i);
    for (int i=0; i<6; i++)		cmp >> Kd(i, i);
    cmp.close();
  }else{
    std::cerr << CMP_FILE << " not opened" << std::endl;
  }

  m_torque.data.length(DOF);
  m_angle.data.length(DOF);

  m_wristForce.data.length(6);

  setRobot(co);

  return RTC::RTC_OK;
}



/*
RTC::ReturnCode_t PA10Controller::onFinalize()
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t PA10Controller::onStartup(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

/*
RTC::ReturnCode_t PA10Controller::onShutdown(RTC::UniqueId ec_id)
{
  return RTC::RTC_OK;
}
*/

RTC::ReturnCode_t PA10Controller::onActivated(RTC::UniqueId ec_id)
{
  std::cout << "on Activated" << std::endl;
  openFiles();

  if(m_angleIn.isNew()){
    m_angleIn.read();
  }

  for(int i=0; i < DOF; ++i){
    qold[i] = m_angle.data[i];
    q_ref[i] = dq_ref[i] = 0.0;
  }

  cur_time = 0.0;

  off = dzerovector(6);

  return RTC::RTC_OK;
}

RTC::ReturnCode_t PA10Controller::onDeactivated(RTC::UniqueId ec_id)
{
  std::cout << "on Deactivated" << std::endl;
  closeFiles();
  return RTC::RTC_OK;
}

RTC::ReturnCode_t PA10Controller::onExecute(RTC::UniqueId ec_id)
{
  if(m_angleIn.isNew()){
    m_angleIn.read();
  }

  if(m_wristForceIn.isNew()){
    m_wristForceIn.read();
  }

  if(!angle.eof()){
    angle >> q_ref[0]; vel >> dq_ref[0];// skip time
    for (int i=0; i<DOF; i++){
      angle >> q_ref[i];
      vel >> dq_ref[i];
    }
  }

  // === PD controller (original) ===

  for(int i=0; i<DOF; i++){
    double q = m_angle.data[i];
    double dq = (q - qold[i]) / TIMESTEP;
    qold[i] = q;
    
    m_torque.data[i] = -(q - q_ref[i]) * Pgain[i] - (dq - dq_ref[i]) * Dgain[i];
  }

  // === PD controller 2 === 
  /*
  moveRobot();
  double q, dq, qr, dqr;
  for(int i=0; i<7; i++){
    q = m_angle.data[i];
    dq = (q - qold[i]) / TIMESTEP;
    qold[i] = q;
	qr = arm_path->joint(i)->q;
	dqr = (qr - angle_o[i])/TIMESTEP;
	angle_o[i] = qr;
    
    m_torque.data[i] = -(q - qr) * Pgain[i] - (dq - dqr) * Dgain[i];
  }

  q = m_angle.data[7];
  dq = (q - qold[7]) / TIMESTEP;
  qold[7] = q;
  qr = fing_path[0]->joint(0)->q;
  dqr = (qr - hando[0])/TIMESTEP;
  hando[0] = qr;

  m_torque.data[7] = -(q - qr) * Pgain[7] - (dq - dqr) * Dgain[7];

  q = m_angle.data[8];
  dq = (q - qold[8]) / TIMESTEP;
  qold[8] = q;
  qr = fing_path[1]->joint(0)->q;
  dqr = (qr - hando[1])/TIMESTEP;
  hando[1] = qr;
  
  m_torque.data[8] = -(q - qr) * Pgain[8] - (dq - dqr) * Dgain[8];
  */

  // === compliance controller 1 ===
  /*
  dmatrix iKp=dzeromatrix(6,6);
  for(int i=0; i<6; i++)
	  iKp(i,i) = 1.0/Kp(i,i);

  static int count=0;
  if(count < 10)
  for(int i=0; i<6; i++){
	  off(i) += m_wristForce.data[i]/10.0;
  }
  count++;

  dvector f_(6);
  for(int i=0; i<6; i++)
	  f_(i) = m_wristForce.data[i] - off(i);

  arm_path->calcJacobian(Jac);

  dmatrix a = prod(trans(Jac), iKp);
  dvector dq_ = prod(a, f_);
  */

  // === direct compliance controller (Yokoi, Maekawa, Tanie '93)===

  dvector mg;
  calcGravityCompensation(mg);

  dvector q_(7), dq_(7), trq(7);

  for(int i=0; i<7; i++){
	  q_(i)  = q_ref[i] - m_angle.data[i];
	  dq_(i) = dq_ref[i] - (m_angle.data[i] - qold[i]) / TIMESTEP;
	  qold[i] = m_angle.data[i];
  }

  arm_path->calcJacobian(Jac);

  dmatrix a = prod(Jac, inverse(Gp));
  dmatrix b = prod(a, trans(Jac));

  dmatrix c = prod(trans(Jac), Kp-inverse(b));
  dmatrix d = prod(c, Jac);
  dvector t1 = prod(Gp+d, q_);

  dmatrix e = prod(Jac, inverse(Gd));
  dmatrix f = prod(e, trans(Jac));

  dmatrix g = prod(trans(Jac), Kd-inverse(f));
  dmatrix h = prod(g, Jac);
  dvector t2 = prod(Gd+h, dq_);

  trq = t1 + t2;

  for(int i=0; i<7; i++){
	  m_torque.data[i] = mg(i) + trq(i);
  }

  //== torque output ==
  m_torqueOut.write();

  //== save data ==
  res << cur_time << "  ";
  for(int i=0; i<DOF; i++)
	  res << m_angle.data[i] << "  ";
  for(int i=0; i<6; i++)
	  res << m_wristForce.data[i] << "  ";
  res << std::endl;

  cur_time += TIMESTEP;

  return RTC::RTC_OK;
}


/*
  RTC::ReturnCode_t PA10Controller::onAborting(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PA10Controller::onError(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PA10Controller::onReset(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PA10Controller::onStateUpdate(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

/*
  RTC::ReturnCode_t PA10Controller::onRateChanged(RTC::UniqueId ec_id)
  {
  return RTC::RTC_OK;
  }
*/

string IntToString(int number)
{
  stringstream ss;
  ss << number;
  return ss.str();
}

void PA10Controller::openFiles()
{
  angle.open(ANGLE_FILE);
  if(!angle.is_open()){
    std::cerr << ANGLE_FILE << " not opened" << std::endl;
  }

  vel.open(VEL_FILE);
  if (!vel.is_open()){
    std::cerr << VEL_FILE << " not opened" << std::endl;
  }

  time_t now = time(NULL);
  struct tm *pnow = localtime(&now);
  int t = pnow->tm_hour*24*60 + pnow->tm_min*60 + pnow->tm_sec;

  string file_name = RESULT_FILE + IntToString(t) + ".dat";

  res.open(file_name.c_str());
  if (!res.is_open()){
    std::cerr << file_name << " not opened" << std::endl;
  }

}

void PA10Controller::closeFiles()
{
  if(angle.is_open()){
    angle.close();
    angle.clear();
  }

  if(vel.is_open()){
    vel.close();
    vel.clear();
  }

  if(res.is_open()){
    res.close();
    res.clear();
  }
}

void PA10Controller::setRobot(BodyPtr _body)
{
        total_dof = _body->numJoints();

		wrist = _body->link("J7");
		base  = _body->link("BASE");
		lhand = _body->link("HAND_L");
		rhand = _body->link("HAND_R");

		arm_path = _body->getJointPath(base, wrist);
		fing_path[0] = _body->getJointPath(wrist, lhand);
		fing_path[1] = _body->getJointPath(wrist, rhand);

		arm_path->joint(0)->q = 0.0;
		arm_path->joint(1)->q = 0.8;
		arm_path->joint(2)->q = 0.0;
		arm_path->joint(3)->q = 0.8;
		arm_path->joint(4)->q = 0.0;
		arm_path->joint(5)->q = 0.8;
		arm_path->joint(6)->q = 1.57;

		fing_path[1]->joint(0)->q = 0.0;
		fing_path[0]->joint(0)->q = -0.04;

		_body->calcForwardKinematics();

        ifstream fp("etc/motion.dat");

		double x;

		while(!fp.eof()){
				fp >> x;  ex_time.push_back(x);
				fp >> x;  x_pos.push_back(x);
				fp >> x;  y_pos.push_back(x);
				fp >> x;  z_pos.push_back(x);
				fp >> x;  roll_angle.push_back(x);
				fp >> x;  pitch_angle.push_back(x);
				fp >> x;  yaw_angle.push_back(x);
				fp >> x;  l_hand.push_back(x);
				fp >> x;  r_hand.push_back(x);
		}

		wrist_p_org = wrist->p;
		wrist_r_org = rpyFromRot(wrist->R);
		lhand_org = fing_path[0]->joint(0)->q;
		rhand_org = fing_path[1]->joint(0)->q;

		for(int i=0; i<arm_path->numJoints(); i++)
				angle_o[i] = arm_path->joint(i)->q;
		for(int i=0; i<2; i++)
					hando[i] = fing_path[i]->joint(0)->q;

		return;
}

bool PA10Controller::moveRobot()
{
		bool ret=true;

		int i=0;
		int T = ex_time.size();
		for(int j=0; j<T; j++){
				if(ex_time[j] < cur_time)
						i++;
		}

		vector3 wrist_p, wrist_r;
		double hand[2];
		double m_pi = 3.141592;

		if(i==0){
				wrist_p = wrist_p_org(0) + (x_pos[i]-wrist_p_org(0))/2.0*(1.0 - cos(m_pi*(cur_time/ex_time[i])) ),
						  wrist_p_org(1) + (y_pos[i]-wrist_p_org(1))/2.0*(1.0 - cos(m_pi*(cur_time/ex_time[i])) ),
						  wrist_p_org(2) + (z_pos[i]-wrist_p_org(2))/2.0*(1.0 - cos(m_pi*(cur_time/ex_time[i])) ) ;

				wrist_r = wrist_r_org(0) + ( roll_angle[i]-wrist_r_org(0))/2.0*(1.0 - cos(m_pi*(cur_time/ex_time[i])) ),
						  wrist_r_org(1) + (pitch_angle[i]-wrist_r_org(1))/2.0*(1.0 - cos(m_pi*(cur_time/ex_time[i])) ),
						  wrist_r_org(2) + (  yaw_angle[i]-wrist_r_org(2))/2.0*(1.0 - cos(m_pi*(cur_time/ex_time[i])) ) ;
				
				hand[0] = lhand_org + (l_hand[i] - lhand_org)/2.0*(1.0 - cos(m_pi*(cur_time/ex_time[i])) );
				hand[1] = rhand_org + (r_hand[i] - rhand_org)/2.0*(1.0 - cos(m_pi*(cur_time/ex_time[i])) );

		}
		else if(i<T){
				wrist_p = x_pos[i-1] + (x_pos[i]-x_pos[i-1])/2.0*(1.0 - cos(m_pi*(cur_time-ex_time[i-1])/(ex_time[i]-ex_time[i-1])) ),
						  y_pos[i-1] + (y_pos[i]-y_pos[i-1])/2.0*(1.0 - cos(m_pi*(cur_time-ex_time[i-1])/(ex_time[i]-ex_time[i-1])) ),
						  z_pos[i-1] + (z_pos[i]-z_pos[i-1])/2.0*(1.0 - cos(m_pi*(cur_time-ex_time[i-1])/(ex_time[i]-ex_time[i-1])) );

				wrist_r = roll_angle[i-1] + (  roll_angle[i]- roll_angle[i-1])/2.0*(1.0 - cos(m_pi*(cur_time-ex_time[i-1])/(ex_time[i]-ex_time[i-1])) ),
						 pitch_angle[i-1] + ( pitch_angle[i]-pitch_angle[i-1])/2.0*(1.0 - cos(m_pi*(cur_time-ex_time[i-1])/(ex_time[i]-ex_time[i-1])) ),
						   yaw_angle[i-1] + (   yaw_angle[i]-  yaw_angle[i-1])/2.0*(1.0 - cos(m_pi*(cur_time-ex_time[i-1])/(ex_time[i]-ex_time[i-1])) );
				
				hand[0] = l_hand[i-1] + (l_hand[i] - l_hand[i-1])/2.0*(1.0 - cos(m_pi*(cur_time-ex_time[i-1])/(ex_time[i]-ex_time[i-1])) );
				hand[1] = r_hand[i-1] + (r_hand[i] - r_hand[i-1])/2.0*(1.0 - cos(m_pi*(cur_time-ex_time[i-1])/(ex_time[i]-ex_time[i-1])) );
		}
		else{
				wrist_p = x_pos[i-1], y_pos[i-1], z_pos[i-1];

				wrist_r = roll_angle[i-1], pitch_angle[i-1], yaw_angle[i-1];
				
				hand[0] = l_hand[i-1];
				hand[1] = r_hand[i-1];

				ret = false;
		}				

		arm_path->calcInverseKinematics(wrist_p, rotFromRpy(wrist_r));

   		fing_path[0]->joint(0)->q=hand[0];
		fing_path[1]->joint(0)->q=hand[1];

		arm_path->calcForwardKinematics();
   		fing_path[0]->calcForwardKinematics();
		fing_path[1]->calcForwardKinematics();

		return ret;
}

void PA10Controller::calcGravityCompensation(dvector& mg)
{

/*
  Vector3 l0(0,0,0.2), l1(0,0,0.115), l2(0,0,0.28);
  Vector3 l3(0,0,0.17), l4(0,0,0.25);
  Vector3 l5(-0.0025,0,0.25);
  Vector3 l6(0,0,0.08);

  cout << Vector3(base->p + ll0 + R1*ll1 + R1*R2*ll2 + R1*R2*R3*ll3 + R1*R2*R3*R4*ll4 + R1*R2*R3*R4*R5*ll5 + R1*R2*R3*R4*R5*R6*ll6 ) << std::endl; 

  rg1 = l(0) + R1*rg(0);
  rg2 = l(0) + R1*r(1) + R1*R2*rg(1);
  rg3 = l(0) + R1*r(1) + R1*R2*r(2) + R1*R2*R3*rg(2);
  rg4 = l(0) + R1*r(1) + R1*R2*r(2) + R1*R2*R3*r(3) + R1*R2*R3*R4*rg(3);
  rg5 = l(0) + R1*r(1) + R1*R2*r(2) + R1*R2*R3*r(3) + R1*R2*R3*R4*r(4) + R1*R2*R3*R4*R5*rg(4);
  rg6 = l(0) + R1*r(1) + R1*R2*r(2) + R1*R2*R3*r(3) + R1*R2*R3*R4*r(4) + R1*R2*R3*R4*R5*r(5) + R1*R2*R3*R4*R5*R6*rg(5);
  rg7 = l(0) + R1*r(1) + R1*R2*r(2) + R1*R2*R3*r(3) + R1*R2*R3*R4*r(4) + R1*R2*R3*R4*R5*r(5) + R1*R2*R3*R4*R5*R6*r(6) + R1*R2*R3*R4*R5*R6*R7*rg(6);
*/


	int n = arm_path->numJoints();
	mg.resize(n);
	
	dvector q(n);
	for(int i=0; i<n; i++)
		q(i) = arm_path->joint(i)->q;


  double S1=sin(q(0)), C1=cos(q(0));
  double S2=sin(q(1)), C2=cos(q(1));
  double S3=sin(q(2)), C3=cos(q(2));
  double S4=sin(q(3)), C4=cos(q(3));
  double S5=sin(q(4)), C5=cos(q(4));
  double S6=sin(q(5)), C6=cos(q(5));
  double S7=sin(q(6)), C7=cos(q(6));

  Matrix33 R1, R2, R3, R4, R5, R6, R7, dR1, dR2, dR3, dR4, dR5, dR6, dR7;
   R1= C1, -S1, 0.0,  S1,  C1, 0.0, 0.0, 0.0, 1.0;
  dR1=-S1, -C1, 0.0,  C1, -S1, 0.0, 0.0, 0.0, 0.0;
   R2=1.0, 0.0, 0.0, 0.0,  C2, -S2, 0.0,  S2,  C2;
  dR2=0.0, 0.0, 0.0, 0.0, -S2, -C2, 0.0,  C2, -S2;
   R3= C3, -S3, 0.0,  S3,  C3, 0.0, 0.0, 0.0, 1.0;
  dR3=-S3, -C3, 0.0,  C3, -S3, 0.0, 0.0, 0.0, 0.0;
   R4=1.0, 0.0, 0.0, 0.0,  C4, -S4, 0.0,  S4,  C4;
  dR4=0.0, 0.0, 0.0, 0.0, -S4, -C4, 0.0,  C4, -S4;
   R5= C5, -S5, 0.0,  S5,  C5, 0.0, 0.0, 0.0, 1.0;
  dR5=-S5, -C5, 0.0,  C5, -S5, 0.0, 0.0, 0.0, 0.0;
   R6=1.0, 0.0, 0.0, 0.0,  C6, -S6, 0.0,  S6,  C6;
  dR6=0.0, 0.0, 0.0, 0.0, -S6, -C6, 0.0,  C6, -S6;
   R7= C7, -S7, 0.0,  S7,  C7, 0.0, 0.0, 0.0, 1.0;
  dR7=-S7, -C7, 0.0,  C7, -S7, 0.0, 0.0, 0.0, 0.0;

  vector<Vector3> r(n), rg(n), m(n);
  for(int i=0; i<7; i++){
	  r[i]  = arm_path->joint(i)->b;
	  rg[i] = arm_path->joint(i)->c;
	  m[i]  = arm_path->joint(i)->m*9.8;
  }

  Vector3 drg1_dq1 ( dR1*rg[0] );
  Vector3 drg2_dq1 ( dR1*r[1] + dR1*R2*rg[1] );
  Vector3 drg3_dq1 ( dR1*r[1] + dR1*R2*r[2] + dR1*R2*R3*rg[2] );
  Vector3 drg4_dq1 ( dR1*r[1] + dR1*R2*r[2] + dR1*R2*R3*r[3] + dR1*R2*R3*R4*rg[3] );
  Vector3 drg5_dq1 ( dR1*r[1] + dR1*R2*r[2] + dR1*R2*R3*r[3] + dR1*R2*R3*R4*r[4] + dR1*R2*R3*R4*R5*rg[4] );
  Vector3 drg6_dq1 ( dR1*r[1] + dR1*R2*r[2] + dR1*R2*R3*r[3] + dR1*R2*R3*R4*r[4] + dR1*R2*R3*R4*R5*r[5] + dR1*R2*R3*R4*R5*R6*rg[5] );
  Vector3 drg7_dq1 ( dR1*r[1] + dR1*R2*r[2] + dR1*R2*R3*r[3] + dR1*R2*R3*R4*r[4] + dR1*R2*R3*R4*R5*r[5] + dR1*R2*R3*R4*R5*R6*r[6] + dR1*R2*R3*R4*R5*R6*R7*rg[6] );

  Vector3 drg2_dq2 ( R1*dR2*rg[1] );
  Vector3 drg3_dq2 ( R1*dR2*r[2] + R1*dR2*R3*rg[2] );
  Vector3 drg4_dq2 ( R1*dR2*r[2] + R1*dR2*R3*r[3] + R1*dR2*R3*R4*rg[3] );
  Vector3 drg5_dq2 ( R1*dR2*r[2] + R1*dR2*R3*r[3] + R1*dR2*R3*R4*r[4] + R1*dR2*R3*R4*R5*rg[4] );
  Vector3 drg6_dq2 ( R1*dR2*r[2] + R1*dR2*R3*r[3] + R1*dR2*R3*R4*r[4] + R1*dR2*R3*R4*R5*r[5] + R1*dR2*R3*R4*R5*R6*rg[5] );
  Vector3 drg7_dq2 ( R1*dR2*r[2] + R1*dR2*R3*r[3] + R1*dR2*R3*R4*r[4] + R1*dR2*R3*R4*R5*r[5] + R1*dR2*R3*R4*R5*R6*r[6] + R1*dR2*R3*R4*R5*R6*R7*rg[6] );

  Vector3 drg3_dq3 ( R1*R2*dR3*rg[2] );
  Vector3 drg4_dq3 ( R1*R2*dR3*r[3] + R1*R2*dR3*R4*rg[3] );
  Vector3 drg5_dq3 ( R1*R2*dR3*r[3] + R1*R2*dR3*R4*r[4] + R1*R2*dR3*R4*R5*rg[4] );
  Vector3 drg6_dq3 ( R1*R2*dR3*r[3] + R1*R2*dR3*R4*r[4] + R1*R2*dR3*R4*R5*r[5] + R1*R2*dR3*R4*R5*R6*rg[5] );
  Vector3 drg7_dq3 ( R1*R2*dR3*r[3] + R1*R2*dR3*R4*r[4] + R1*R2*dR3*R4*R5*r[5] + R1*R2*dR3*R4*R5*R6*r[6] + R1*R2*dR3*R4*R5*R6*R7*rg[6] );

  Vector3 drg4_dq4 ( R1*R2*R3*dR4*rg[3] );
  Vector3 drg5_dq4 ( R1*R2*R3*dR4*r[4] + R1*R2*R3*dR4*R5*rg[4] );
  Vector3 drg6_dq4 ( R1*R2*R3*dR4*r[4] + R1*R2*R3*dR4*R5*r[5] + R1*R2*R3*dR4*R5*R6*rg[5] );
  Vector3 drg7_dq4 ( R1*R2*R3*dR4*r[4] + R1*R2*R3*dR4*R5*r[5] + R1*R2*R3*dR4*R5*R6*r[6] + R1*R2*R3*dR4*R5*R6*R7*rg[6] );

  Vector3 drg5_dq5 ( R1*R2*R3*R4*dR5*rg[4] );
  Vector3 drg6_dq5 ( R1*R2*R3*R4*dR5*r[5] + R1*R2*R3*R4*dR5*R6*rg[5] );
  Vector3 drg7_dq5 ( R1*R2*R3*R4*dR5*r[5] + R1*R2*R3*R4*dR5*R6*r[6] + R1*R2*R3*R4*dR5*R6*R7*rg[6] );

  Vector3 drg6_dq6 ( R1*R2*R3*R4*R5*dR6*rg[5] );
  Vector3 drg7_dq6 ( R1*R2*R3*R4*R5*dR6*r[6] + R1*R2*R3*R4*R5*dR6*R7*rg[6] );

  Vector3 drg7_dq7 ( R1*R2*R3*R4*R5*R6*dR7*rg[6] );

  Vector3 z3(0, 0, 1);

  mg(0) = dot(Vector3(m[0]*drg1_dq1 + m[1]*drg2_dq1 + m[2]*drg3_dq1 + m[3]*drg4_dq1 + m[4]*drg5_dq1 + m[5]*drg6_dq1 + m[6]*drg7_dq1), z3);
  mg(1) = dot(Vector3(                m[1]*drg2_dq2 + m[2]*drg3_dq2 + m[3]*drg4_dq2 + m[4]*drg5_dq2 + m[5]*drg6_dq2 + m[6]*drg7_dq2), z3);
  mg(2) = dot(Vector3(                                m[2]*drg3_dq3 + m[3]*drg4_dq3 + m[4]*drg5_dq3 + m[5]*drg6_dq3 + m[6]*drg7_dq3), z3);
  mg(3) = dot(Vector3(                                                m[3]*drg4_dq4 + m[4]*drg5_dq4 + m[5]*drg6_dq4 + m[6]*drg7_dq4), z3);
  mg(4) = dot(Vector3(                                                                m[4]*drg5_dq5 + m[5]*drg6_dq5 + m[6]*drg7_dq5), z3);
  mg(5) = dot(Vector3(                                                                                m[5]*drg6_dq6 + m[6]*drg7_dq6), z3);
  mg(6) = dot(Vector3(                                                                                                m[6]*drg7_dq7), z3);

  return;

}

extern "C"
{

  DLL_EXPORT void PA10ControllerInit(RTC::Manager* manager)
  {
    coil::Properties profile(PA10Controller_spec);
    manager->registerFactory(profile,
                             RTC::Create<PA10Controller>,
                             RTC::Delete<PA10Controller>);
  }

};


