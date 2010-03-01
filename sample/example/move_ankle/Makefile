all: move_ankle

CXX_FLAGS = `pkg-config --cflags openhrp-aist`
LINK_FLAGS = `pkg-config --libs openhrp-aist`

%.o: %.cpp
	$(CXX) -c $(CXX_FLAGS) -o $@ $<

move_ankle : move_ankle.o
	$(CXX) -o $@ $^ $(LINK_FLAGS)

clean:
	rm *.o move_ankle
