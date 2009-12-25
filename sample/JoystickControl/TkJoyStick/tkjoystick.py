#!/usr/bin/env python
# -*- coding: euc-jp -*-

#
# @brief tk joystick
# @date $Date$
# @author Norkai Ando <n-ando@aist.go.jp>
#
# Copyright (C) 2007
#     Noriaki Ando
#     Task-intelligence Research Group,
#     Intelligent Systems Research Institute,
#     National Institute of
#         Advanced Industrial Science and Technology (AIST), Japan
#     All rights reserved.
#
# $Id$
#

# $Log$
#

from Tkinter import *
import time
import math
import mutex
import sys

class ToggleItem:
    def __init__(self):
        self.active = True

    def activate(self):
        self.active = True
        self.draw()

    def deactivate(self):
        self.active = False
        self.delete()

    def toggle(self):
        if self.active:
            self.deactivate()
        else:
            self.activate()


class CanvasText(ToggleItem):
    def __init__(self, canvas, text, x, y):
        ToggleItem.__init__(self)
        self.canvas = canvas
        self.id = self.canvas.create_text(x, y, text=text)
        self.text = text
        self.x = x
        self.y = y
        self.draw_text(x, y, text)

    def draw(self):
        if self.active == False: return
        self.delete()
        self.id = self.canvas.create_text(self.x, self.y, text=self.text)

    def draw_text(self, x, y, text):
        self.x = x
        self.y = y
        self.text = text
        self.draw()

    def delete(self):
        self.canvas.delete(self.id)


class CanvasAxis(ToggleItem):
    def __init__(self, canvas, width, height):
        ToggleItem.__init__(self)
        self.x0 = width/2
        self.y0 = height/2
        self.width = width
        self.height = height
        self.canvas = canvas
        self.id = [None] * 4
        self.draw()

    def draw(self):
        if self.active == False: return
        self.delete()
        self.id[0] = self.canvas.create_line(0, self.height/2, 
                                             self.width, self.height/2)
        self.id[1] = self.canvas.create_text(self.width - 10,
                                             self.height/2 + 10,
                                             text="x")
        self.id[2] = self.canvas.create_line(self.width/2, 0, 
                                             self.width/2, self.height)
        self.id[3] = self.canvas.create_text(self.width/2 + 10,
                                             + 10, text="y")
        return

    def delete(self):
        for i in self.id:
            self.canvas.delete(i)

class CanvasCircle(ToggleItem):
    def __init__(self, canvas, x, y, width, height, pitch):
        ToggleItem.__init__(self)
        self.canvas = canvas
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.pitch = pitch
        self.id = []
        self.draw()

    def draw(self):
        if self.active == False: return
        self.delete()
        if self.pitch == 0:
            circnum = 0
        else:
            circnum = max(self.width, self.height) / self.pitch
        circrange = range(int(circnum))
        circrange.reverse()
        for i in circrange:
            x0 = self.x - self.pitch * i
            y0 = self.y - self.pitch * i
            x1 = self.x + self.pitch * i
            y1 = self.y + self.pitch * i
            if i % 2 == 0:
                color = "#dddddd"
            else:
                color = "#eeeeee"
            id = self.canvas.create_oval(x0, y0, x1, y1, fill=color,
                                              outline="#ffffff")
            self.id.append(id)
        id = self.id
        id.reverse()
        for i in id:
            self.canvas.tag_lower(i)

    def delete(self):
        for c in self.id:
            self.canvas.delete(c)

    def set_pitch(self, pitch):
        self.pitch = pitch
        self.draw()

class CanvasLine(ToggleItem):
    def __init__(self, canvas, x0, y0, x1, y1, color, width):
        ToggleItem.__init__(self)
        self.canvas = canvas
        self.x0 = x0
        self.y0 = y0
        self.x1 = y1
        self.y1 = y1
        self.color = color
        self.width = width
        self.id = self.canvas.create_line(self.x0, self.y0, self.x1, self.y1,
                                          fill=self.color, width=self.width)
        self.draw()

    def draw(self):
        if self.active == False: return
        self.delete()
        self.id = self.canvas.create_line(self.x0, self.y0, self.x1, self.y1,
                                          fill=self.color, width=self.width)
    def draw_line(self, x0, y0, x1, y1):
        self.x0 = x0
        self.y0 = y0
        self.x1 = x1
        self.y1 = y1
        self.draw()

    def delete(self):
        self.canvas.delete(self.id)

class Stick:
    def __init__(self, canvas, x, y, r, **key):
        self.canvas = canvas
        # ジョイスティックID
        self.key = key
        self.id = self.canvas.create_oval(x-r, y-r, x+r, y+r, **key)
        # 中心からジョイスティックへの線ID
        self.line = None
        # (x,y) テキスト表示ID
        self.xy_text = None
        # (r,th) テキスト表示ID
        self.pol_text = None

        # 画面座標系から画面ジョイスティック座標系へのオフセット
        self.offsetx = x
        self.offsety = y

        # 画面ジョイスティック座標系でのジョイスティック位置
        self.x = 0
        self.y = 0

        # ジョイスティック座標系でのジョイスティック位置
        self.pos_x = 0
        self.pos_y = 0

        self.coffx = 0
        self.coffy = 0

        # ジョイスティックへのバインド
        self.make_binds()
        # テキスト描画

    def set_on_drag_start(self, func):
        self.on_drag_start = func

    def set_on_dragging(self, func):
        self.on_dragging = func

    def set_on_drag_end(self, func):
        self.on_drag_end = func

    def make_binds(self):
        self.canvas.tag_bind(self.id, '<1>', self.drag_start)
        self.canvas.tag_bind(self.id, '<Button1-Motion>', self.dragging)
        self.canvas.tag_bind(self.id, '<Button1-ButtonRelease>', self.drag_end)
        
    def drag_start(self, event):
        # クリック位置のオフセットを計算
        x1 = event.x - self.offsetx
        y1 = event.y - self.offsety
        self.coffx = x1 - self.x
        self.coffy = y1 - self.y

        self.calc_pol(self.pos_x, self.pos_y)
#        self.draw_text()

        # コールバック
        if self.on_drag_start != None:
            self.on_drag_start((self.pos_x, self.pos_y), (self.r, self.th))
        
    def dragging(self, event):
        # ドラッグの移動量
        x1 = event.x - self.offsetx
        y1 = event.y - self.offsety
        dx = (x1 - self.x) - self.coffx
        dy = (y1 - self.y) - self.coffy

        # 円を移動
        self.canvas.move(self.id, dx, dy)
        self.canvas.tag_raise(self.id)
        # ジョイスティック位置を計算
        self.x = x1 - self.coffx
        self.y = y1 - self.coffy
        self.pos_x = self.x
        self.pos_y = -self.y 
        self.calc_pol(self.pos_x, self.pos_y)

        # コールバック
        if self.on_dragging != None:
            self.on_dragging((self.pos_x, self.pos_y), (self.r, self.th))

    def drag_end(self, event):
        x1 = event.x - self.offsetx
        y1 = event.y - self.offsety
        # 戻すための移動量
        dx = x1 - self.coffx
        dy = y1 - self.coffy
        self.canvas.move(self.id, -dx, -dy)
        self.x = 0
        self.y = 0
        self.pos_x = 0
        self.pos_y = 0
        self.calc_pol(self.pos_x, self.pos_y)
        # コールバック
        if self.on_drag_end != None:
            self.on_drag_end((self.pos_x, self.pos_y), (self.r, self.th))

    def calc_pol(self, x, y):
        r = math.hypot(x, y)
        if r == 0:
            th = 720.0
        elif y >= 0.0:
            th = math.acos((x * 1.0) / (r * 1.0)) * 180.0 / math.pi
        elif y < 0.0:
            th = math.acos((x * 1.0) / (r * 1.0)) * -180.0 / math.pi
        self.r = r
        self.th = th
        return (r, th)

    def get_pos(self):
        return self.pos_x, self.pos_y, self.r, self.th


class TkJoystick(Frame):
    def __init__(self, r=10, width=300, height=300, master=None):
        Frame.__init__(self, master)

        self.pos_x = 0.0
        self.pos_y = 0.0
        self.pol_r = 0.0
        self.pol_th = 0.0

        self.stick_r = r

        self.width = width
        self.height = height

        self.wd = 160

        self.x0 = width/2
        self.y0 = height/2

        self.xentry = ""
        self.yentry = ""
        self.rentry = ""
        self.thentry = ""

        # scale variable
        self.scale_var = DoubleVar()
        self.scale_var.set(1.0)

        # checkbutton variables
        self.vline_check = StringVar()
        self.vline_check.set("on")
        self.axis_check = StringVar()
        self.axis_check.set("on")
        self.circ_check = StringVar()
        self.circ_check.set("on")
        self.xy_check = StringVar()
        self.xy_check.set("on")
        self.pol_check = StringVar()
        self.pol_check.set("on")

        # x-y entry variables
        self.xentry = StringVar()
        self.xentry.set("0.0")
        self.yentry = StringVar()
        self.yentry.set("0.0")

        # polarpos entry variables
        self.rentry = StringVar()
        self.rentry.set("0.0")
        self.thentry = StringVar()
        self.thentry.set("0.0")

        # text position
        self.xytext_x = self.width - 50
        self.xytext_y = self.height - 25
        self.poltext_x = self.width - 60
        self.poltext_y = self.height - 10

        # vector line
        self.vline_color = "#aaaaaa"
        self.vline_width = 2
        self.default_pitch = 50

        # callback function
        self.on_update = None

        self.init()
        self.pack()


    def init(self):
        self.canvas = Canvas(self, bg="white", \
                                 width = self.width, height = self.height)
        self.canvas.pack(side=LEFT)

        # coaxial pattern
        self.can_circle = CanvasCircle(self.canvas, 
                                   self.x0, self.y0,
                                   self.width, self.height, pitch=50)

        # axis
        self.can_axis = CanvasAxis(self.canvas, self.width, self.height)

        # x-y text
        text = 'x: %4d, y: %4d' % (0, 0)
        self.can_xytext = CanvasText(self.canvas, text,
                                     self.xytext_x, self.xytext_y)

        # polar position text
        text = 'r: %4.2f, th: NaN' % (0.0)
        self.can_poltext = CanvasText(self.canvas, text,
                                      self.poltext_x, self.poltext_y)
                           
        self.can_vline = CanvasLine(self.canvas, self.x0, self.y0,
                                    self.x0, self.y0,
                                    self.vline_color, self.vline_width)

        # joystick circle
        self.stick = Stick(self.canvas, self.width/2, self.height/2,
                           self.stick_r,
                           fill="#999999", width=1)
        self.stick.set_on_drag_start(self.on_pos_update)
        self.stick.set_on_dragging(self.on_pos_update)
        self.stick.set_on_drag_end(self.on_pos_update)


        # right side widgets
        self.frame = Frame(self, height=self.height, width=self.wd)
        self.frame.pack(side=LEFT, fill=Y, padx=3, pady=3)
        self.create_scale(self.frame).pack(side=TOP, fill=Y)
        self.create_checkbutton(self.frame).pack(side=TOP, fill=Y)
        self.create_pollabel(self.frame).pack(side=BOTTOM, fill=Y)
        self.create_xylabel(self.frame).pack(side=BOTTOM, fill=Y)
        return

    def create_scale(self, frame):
        f = Frame(frame, bd=2, relief=GROOVE)
        dummy = Frame(f, width=self.wd)
        dummy.pack(side=TOP)
        sl = Scale(f, from_=0, to=10, resolution=0.01,
                   label="Scale Factor", command=self.on_scale,
                   variable=self.scale_var, orient=HORIZONTAL)
        bt = Button(f, text="Reset Scale", command=self.reset_scale)
        sl.pack(side=TOP, fill=X)
        bt.pack(side=TOP, fill=X)
        return f

    def on_scale(self, val):
        v =  float(val)
        if v == 0.0:
            pitch = 0
        else:
            pitch = self.default_pitch/v
        self.can_circle.set_pitch(pitch)

    def create_checkbutton(self, frame):
        f = Frame(frame, bd=2, relief=GROOVE)
        dummy = Frame(f, width=self.wd)
        dummy.pack(side=TOP)
        vec = Checkbutton(f, text="Vector Line",
                           onvalue="on", offvalue="off",
                           justify=LEFT, anchor=W,
                           variable=self.vline_check,
                           command=self.can_vline.toggle)
        axis = Checkbutton(f, text="Axis",
                           onvalue="on", offvalue="off",
                           justify=LEFT, anchor=W,
                           variable=self.axis_check,
                           command=self.can_axis.toggle)
        circ = Checkbutton(f, text="Background",
                           onvalue="on", offvalue="off",
                           justify=LEFT, anchor=W,
                           variable=self.circ_check,
                           command=self.can_circle.toggle)
        xy   = Checkbutton(f, text="X-Y position", 
                           onvalue="on", offvalue="off",
                           justify=LEFT, anchor=W,
                           variable=self.xy_check,
                           command=self.can_xytext.toggle)
        pol  = Checkbutton(f, text="Polar postion",
                           onvalue="on", offvalue="off",
                           justify=LEFT, anchor=W,
                           variable=self.pol_check,
                           command=self.can_poltext.toggle)
        for w in [vec, axis, circ, xy, pol]:
            w.pack(side=TOP, anchor=W, fill=X)
        return f

    def create_xylabel(self, frame):
        f = Frame(frame)
        dummy = Frame(f, width=self.wd)
        dummy.pack(side=TOP)
        xl = Label(f, text="x: ", width=3)
        xe = Entry(f, width=7, textvariable=self.xentry,
                   justify=RIGHT, relief=GROOVE)
        yl = Label(f, text="y: ", width=3)
        ye = Entry(f, width=7, textvariable=self.yentry,
                   justify=RIGHT, relief=GROOVE)

        for w in [ye, yl, xe, xl]:
            w.pack(side=RIGHT, fill=X)
        return f

    def create_pollabel(self, frame):
        f = Frame(frame, width=self.wd)
        dummy = Frame(f, width=self.wd)
        dummy.pack(side=TOP)
        rl = Label(f, text="r: ", width=3)
        re = Entry(f, width=7, textvariable=self.rentry,
                   justify=RIGHT, relief=GROOVE)
        thl = Label(f, text="th:", width=3)
        the = Entry(f, width=7, textvariable=self.thentry,
                    justify=RIGHT, relief=GROOVE)
        for w in [the, thl, re, rl]:
            w.pack(side=RIGHT, fill=X)
        return f

    def reset_scale(self):
        self.scale_var.set(1.)
        pitch = self.default_pitch/1.0
        self.can_circle.set_pitch(pitch)
        
    def get_pos(self):
        return self.pos_x, self.pos_y, self.pol_r, self.pol_th

    def on_pos_update(self, pos, pol):
        self.pos_x = pos[0] * self.scale_var.get()
        self.pos_y = pos[1] * self.scale_var.get()
        self.pol_r = pol[0] * self.scale_var.get()
        self.pol_th = pol[1]

        xt = '%4d' % (self.pos_x)
        yt = '%4d' % (self.pos_y)
        rt = '%5.2f' % (self.pol_r)
        if pol[1] > 360: tht = 'NaN'
        else: tht = '%5.2f' % self.pol_th

        self.xentry.set(xt)
        self.yentry.set(yt)
        self.rentry.set(rt)
        self.thentry.set(tht)

        text = 'x: %4d, y: %4d' % (pos[0], pos[1])
        self.can_xytext.draw_text(self.xytext_x, self.xytext_y, text)
        if pol[1] > 360:
            text = 'r: %5.2f, th: NaN' % (pol[0])
        else:
            text = 'r: %5.2f, th: %5.2f' % (pol[0], pol[1])
        self.can_poltext.draw_text(self.poltext_x, self.poltext_y, text)

        self.can_vline.draw_line(self.x0, self.y0,
                                 self.x0 + pos[0],
                                 self.y0 - pos[1])

        if self.on_update != None:
            self.on_update((self.pos_x, self.pos_y),
                           (self.pol_r, self.pol_th))

    def set_on_update(self, func):
        self.on_update = func

def test ():
    m = TkJoystick()

    while 1:
        m.update()
        print m.get_pos()


if  __name__ == '__main__': test()
