*****************************************************************************
*
*	Capulin UT DSP.asm
*  	Mike Schoonover 5/19/2009
*
*	Description
*	===========
*
*	This is the DSP software for the Capulin Series UT boards.
*	
*	Target DSP type is Texas Instruments TMS320VC5441.
*
*	Instructions
*	============
*
* IMPORTANT NOTE
*
* To assemble the program for use with the Capulin system, use the
* batch file located in the root source folder, such as:
* 	"aa Assemble Capulin UT DSP.bat"
* This batch files creates the necessary output hex file for loading
* into the DSPs.  Assembling in the IDE does NOT create the hex file.
* Compile and debug in the IDE, then execute the assemble batch file
* from Windows Explorer, then execute the copy batch file to copy the
* hex file to the proper location for use by the Java program.
*
* When creating a Code Composer project, the root source folder should
* be used as the project folder.  The root folder is the folder containing
* all the source code and the assemble batch file mentioned above.  If
* care is not taken, Composer will create another folder inside the root
* folder which will make things more confusing.
*
* Use Project/New, type in the project name, then browse to the root source
* folder for the "Location".  Composer may try to add the project name to
* the root folder -- remove the project name from the end of the path list.
* Double check the "Location" path to ensure that it ends with the root
* source folder before clicking "Finish".
* 
* Two nearly identical .cmd files are used.  The one used by the assembler
* batch file mentioned above uses one to load the .obj file from the root
* source folder where the batch file specifies it to be placed.  The other
* cmd file has a name like "Capulin UT DSP - Debug - use in Code Composer.cmd"
*  and is loaded into the Composer project so that it loads the .obj file
* from the "Debug" folder where Composer places it by default for debug mode.
*
* NOTE: Any changes to one .cmd file should be copied to the other.
*
* After creating a project, choose Project/Build Options/Linker tab/Basic
* and set "Autoinit Model" to "No Autoinitialization" to avoid the undefined
* warning for "_c_int00".
*
* To debug in Composer, use Build All, then File/Load Program to load the
* .out file from the root source directory.  Each time the project is rebuilt
* use File/Reload Program.  After each load or reload, use Debug/Reset CPU
* to refresh the disassembly and code windows.  Use View/Memory to view
* the memory data.
*
* Debug setup and testing code is contained in "Capulin UT DSP Debug.asm",
* the code in which is not compiled unless the assembler symbol "debug" is
* defined in "Globals.asm". Search for the phrase "debugCode" and read the
* notes in "Capulin UT DSP Debug.asm" for more info.
*
* When installing Code Composer, you must the Code Composer Studio Setup
* program first.  From the center column, select the C5410 Device Simulator
* and click "Add", then "Save & Quit".  The '5410 does not fully simulate the
* '5441, but it has most of the features.  It only simulates one core of the
* four contained in the '5441.
*
******************************************************************************

	.mmregs


	.include "Globals.asm"	; global symbols such as "debug"

	.include	"TMS320VC5441.asm"

	.global	pointToGateInfo
	.global	calculateGateIntegral
	.global	BRC
	.global	Variables1
	.global	scratch1
	.global	debugCode

	.global	setADSampleSize
	.global fpgaADSampleBufEnd
	.global SERIAL_PORT_RCV_BUFFER
	.global	FPGA_AD_SAMPLE_BUFFER
	.global	GATES_ENABLED
	.global	setFlags1
	.global	getPeakData

;-----------------------------------------------------------------------------
; Miscellaneous Defines
;

; IMPORTANT NOTE: SERIAL_PORT_RCV_BUFFER is a circular buffer and must be
;  placed at an allowed boundary based on the size of the buffer - see manual
; "TMS320C54x DSP Reference Set Volume 5: Enhanced Peripherals" for details.
;

SERIAL_PORT_RCV_BUFFER		.equ	0x3000	; circular buffer for serial port in
SERIAL_PORT_RCV_BUFSIZE		.equ	0x100	; size of in buffer
SERIAL_PORT_XMT_BUFFER		.equ	0x3500	; buffer for serial port out


ASCAN_BUFFER				.equ	0x3700	; AScan data set stored here
FPGA_AD_SAMPLE_BUFFER		.equ	0x4000	; FPGA stores AD samples here
PROCESSED_SAMPLE_BUFFER		.equ	0x8000	; processed data stored here

; bits for flag1 variable
TRANSMITTER_ACTIVE			.equ	0x0001	; transmitter active flag
GATES_ENABLED				.equ	0x0002	; gates enabled flag
DAC_ENABLED					.equ	0x0004	; DAC enabled flag
ASCAN_FAST_ENABLED			.equ	0x0008	; AScan fast version enabled flag
ASCAN_SLOW_ENABLED			.equ	0x0010	; AScan slow version enabled flag

POSITIVE_HALF				.equ	0x0000
NEGATIVE_HALF				.equ	0x0001
FULL_WAVE					.equ	0x0002
RF_WAVE						.equ	0x0003

;bits for gate and DAC flags
GATE_ACTIVE						.equ	0x0001
GATE_REPORT_NOT_EXCEED			.equ	0x0002
GATE_MAX_MIN					.equ	0x0004
GATE_WALL_START					.equ	0x0008
GATE_WALL_END					.equ	0x0010
GATE_FIND_CROSSING				.equ	0x0020
GATE_USES_TRACKING				.equ	0x0040
GATE_FIND_PEAK					.equ	0x0080
GATE_FOR_INTERFACE				.equ	0x0100
GATE_INTEGRATE_ABOVE_GATE		.equ	0x0200
GATE_QUENCH_ON_OVERLIMIT		.equ	0x0400

;bit masks for gate results data flag

HIT_COUNT_MET				.equ	0x0001
MISS_COUNT_MET				.equ	0x0002
GATE_EXCEEDED				.equ	0x0004

;bit masks for processingFlags1

IFACE_FOUND					.equ	0x0001
WALL_START_FOUND			.equ	0x0002
WALL_END_FOUND				.equ	0x0004

;size of buffer entries

;WARNING: Adjust these values any time you add more bytes to the buffers.

GATE_PARAMS_SIZE			.equ	14
GATE_RESULTS_SIZE			.equ	12
DAC_PARAMS_SIZE				.equ	9

; end of Miscellaneous Defines
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; Message / Command IDs
; Should match settings in host computer.
;

DSP_NULL_MSG_CMD				.equ	0
DSP_GET_STATUS_CMD 				.equ	1
DSP_SET_GAIN_CMD 				.equ	2
DSP_GET_ASCAN_BLOCK_CMD			.equ	3
DSP_GET_ASCAN_NEXT_BLOCK_CMD	.equ	4
DSP_SET_AD_SAMPLE_SIZE_CMD		.equ	5
DSP_SET_DELAYS					.equ	6
DSP_SET_ASCAN_RANGE				.equ	7
DSP_SET_GATE					.equ	8
DSP_SET_GATE_FLAGS				.equ	9
DSP_SET_DAC						.equ	10
DSP_SET_DAC_FLAGS				.equ	11
DSP_SET_HIT_MISS_COUNTS			.equ	12
DSP_GET_PEAK_DATA				.equ	13
DSP_SET_RECTIFICATION			.equ	14
DSP_SET_FLAGS1					.equ	15
DSP_CLEAR_FLAGS1				.equ	16
DSP_SET_GATE_SIG_PROC_THRESHOLD	.equ	17
DSP_ACKNOWLEDGE					.equ	127

; end of Message / Command IDs
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; Vectors
;
; For the TMS320VC5441, each dsp core begins execution at the reset vector
; at 0xff80.
;
; NOTE: The vector table can be relocated by changing the IPTR in the PMST
; register.  The 9 bit IPTR pointer selects the 128 word program page where
; the vectors start.  At reset, it is set to 0x1ff which puts the vector table
; at 0xff80 on page 0 as that page is the default at reset.  Since the reset
; vector is always at offset 0 in this table, it will always be at 0xff80.
;
; If the program memory page is changed via the XPC register, a copy of the
; vector table must exist on the page being switched to if interrrupts are
; active during that time.  Alternatively, the vector table can be relocated
; at run time to a section which is constant regardless of which page is
; active.  For instance, on the TMS320VC5441 for core A, MPDA is always present
; in the lower half of program memory when OVLY=0 and MPAB3 is always present
; when OVLY=1.  The vectors can be copied to one of these pages (which depends
; on the setting of OVLY) and will always be active regardless of the currently
; selected page.  For MPD*, words 0h - 60h are reserved so the table should not
; be relocated to those addresses.
;
; Similarly, any code branched to by the interrupt vector must also be available
; when the interrupt occurs.
;
; Table 3-26 of the TMS320VC5441 Data Manual shows the offsets for each vector
; in the vector table.  These are not the actual addresses - add these values
; to the first word of the page being pointed to by IPTR.  If IPTR is 0x1ff,
; the first vector will be at 0x1ff * 128 = 0xff80.  The reset vector is listed
; as being at 0x00 so it can be found at 0xff80 + 0x00 = 0xff80.
;

	.sect	"vectors"				; link this at 0xff80

	b		main

; end of Vectors
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; Variables - uninitialized

	.bss	Variables1,1			; used to mark the first page of variables
	
	
	.bss	heartBeat,1				; incremented constantly to show that program
									; is running
	
	
	.bss	flags1,1				; bit 0 : serial port transmitter active
									; bit 1 : Gates Enabled
									; bit 2 : DAC Enabled
									; bit 3 : AScan Enabled

	.bss	softwareGain,1			; gain multiplier for the signal
	.bss	adSampleSize,1			; size of the unpacked data set from the FPGA
	.bss	adSamplePackedSize,1	; size of the data set from the FPGA
	.bss	fpgaADSampleBufEnd,1	; end of the buffer where FPGA stores A/D samples
	.bss	coreID,1				; ID number of the DSP core (1-4)
	.bss	getAScanBlockPtr,1		; points to next data to send to host
	.bss	getAScanBlockSize,1		; number of AScan words to transfer per packet
	.bss	hardwareDelay1,1		; high word of FPGA hardware delay
	.bss	hardwareDelay0,1		; low word of FPGA hardware delay
	.bss	aScanDelay,1			; delay for start of AScan
	.bss	aScanScale,1			; compression scale for AScan
	.bss	aScanChunk,1			; number of input points to scan for each output point
	.bss	aScanSlowBatchSize,1	; number of output data words to process in each batch
	.bss	aScanMin,1				; stores min values during compression
	.bss	aScanMinLoc,1			; location of min peak
	.bss	aScanMax,1				; stores max values during compression	
	.bss	aScanMaxLoc,1			; location of max peak
	.bss	trackCount,1			; location tracking value
	.bss	freeTimeCnt1,1			; high word of free time counter
	.bss	freeTimeCnt0,1			; low word of free time counter
	.bss	freeTime1,1				; high word of free time value
	.bss	freeTime0,1				; low word of free time value

; the next block is used by the function processAScanSlow for variable storage

	.bss	inBufferPASS,1			; pointer to data in input buffer
	.bss	outBufferPASS,1			; pointer to data in output buffer
	.bss	totalCountPASS,1		; counts total number of output data points

; end of PASS variables

	.bss	serialPortInBufPtr,1	; points to next position of in buffer
	.bss	reSyncCount,1			; tracks number of times reSync required

	.bss	hitCount,1				; gate violations required to flag
	.bss	missCount,1				; gate misses required to flag

	.bss	dma3Source,1			; used to write to program memory via DMA

	.bss	frameCount1,1			; MSB of A/D data sets count
	.bss	frameCount0,1			; LSB of A/D data sets count
	.bss	frameSkipErrors,1		; missing data set error count
	.bss	frameCountFlag,1		; stores previous count flag from FPGA

	.bss	interfaceGateIndex,1	; index number of the interface gate if in use
	.bss	processingFlags1,1		; flags used by signal processing functions

	.bss	wallStartGateIndex,1	; index number of the wall start gate if used
	.bss	wallStartGateInfo,1		; pointer to wall start gate info
	.bss	wallStartGateResults,1	; pointer to wall start gate results
	.bss	wallStartGateLevel,1	; level of the wall start gate stored for quick access

	.bss	wallEndGateIndex,1		; index number of the wall end gate if used
	.bss	wallEndGateInfo,1		; pointer to wall end gate info
	.bss	wallEndGateResults,1	; pointer to wall end gate results
	.bss	wallEndGateLevel,1		; level of the wall end gate stored for quick access

	.bss	scratch1,1				; scratch variable for any temporary use
	.bss	scratch2,1				; scratch variable for any temporary use
	.bss	scratch3,1				; scratch variable for any temporary use
	.bss	scratch4,1				; scratch variable for any temporary use
	.bss	scratch5,1				; scratch variable for any temporary use

	; Wall Peak Buffer Notes
	;
	; This buffer holds data for the crossing points of the two gates used
	; to calculate the wall thickness.  The data for the positions
	; representing the thinnest and the thickest wall are stored.
	;
	; With 66 MHz sampling, the period between samples is 15 ns, or
	; 0.015 us.  Assuming the speed of sound in steel is 0.233 inches/us,
	; 15 ns gives a resolution of 0.003495 inches.  Since two measurements
	; are required to calculate the wall, the resolution is twice this as
	; each measurement has that resolution.
	;
	; To improve the resolution, an extrapolation is made to find a point
	; between the 15 ns samples which more closely approximates the point
	; in time where the signal crosses the gate.  This is done by assuming
	; the line between two samples is nearly linear and calculating the
	; fraction of the amplitude distance between the two points where the
	; gate level falls.  This fraction can then be used as the fraction
	; of the time between the two points where the signal crosses the gate.
	; This assumes that the crossing occurs on the near vertical rising
	; edge of the signal where the extrapolated line between two points
	; is a nearly linear portion of the sine wave.
	;
	; The min and max peak distances between the gate crossings are stored
	; and passed back when the host requests.  The distance between the
	; point just before the crossover of the starting gate and the point
	; just before the crossover of the ending gate is stored along with
	; the numerator and denominator fraction for both crossovers - this
	; is done for both the max and the min peak.
	;
	; Each new measurement is compared with the min and max peaks. If
	; the distance for the new value is not equal to the old stored
	; peak, a new min or max peak is stored as appropriate.  If the
	; distance is the same, the fractional portions are compared to
	; determine which is bigger or smaller.
	;
	; To avoid division, the numerators and denominators are stored
	; and all values are normalized to have the same denominators
	; so the numerators can be compared.  This is done on each
	; measurement and comparison.
	;
	; word  0:	current value - whole number distance between crossovers
	; word  1:	current value - numerator first crossover
	; word  2:	current value - denominator first crossover 
	; word  3:	current value - numerator second crossover
	; word  4:	current value - denominator second crossover
	;
	; word  5:	max peak - whole number distance between crossovers
	; word  6:	max peak - numerator first crossover
	; word  7:	max peak - denominator first crossover
	; word  8:	max peak - numerator second crossover
	; word  9:	max peak - denominator second crossover 
	; word 10:	max peak - tracking location
	;
	; word 11:	min peak - whole number distance between crossovers
	; word 12:	min peak - numerator first crossover
	; word 13:	min peak - denominator first crossover
	; word 14:	min peak - numerator second crossover
	; word 15:	min peak - denominator second crossover 
	; word 16:	min peak - tracking location
	;
	; word 17:	current value - normalized numerator first crossover
	; word 18:	current value - normalized denominator first crossover 
	; word 19:	current value - normalized numerator second crossover
	; word 20:	current value - normalized denominator second crossover 
	;
	; word 21:	max peak - normalized numerator first crossover
	; word 22:	max peak - normalized denominator first crossover
	; word 23:	max peak - normalized numerator second crossover
	; word 24:	max peak - normalized denominator second crossover 
	;
	; word 25:	min peak - normalized numerator first crossover
	; word 26:	min peak - normalized denominator first crossover
	; word 27:	min peak - normalized numerator second crossover
	; word 28:	min peak - normalized denominator second crossover 
	;

	.bss	wallPeakBuffer, 29

	; Gate Buffer Notes
	;
	; The gatesBuffer section is for storing 10 gates.
	; Each gate is defined by 14 words each:
	;
	; word  0: first gate ID number (if interface gate is in use, it must always be gate 0)
	;			   (upper two bits used to store pointer to next averaging buffer)
	; word  1: gate function flags (see below)
	; word  2: gate start location MSW
	; word  3: gate start location LSW
	; word  4: gate adjusted start location
	; word  5: gate width / 3
	; word  6: gate level
	; word  7: gate hit count (number of consecutive violations before flag)
	; word  8: gate miss count (number of consecutive non-violations before flag)
	; word  9: Threshold 1
	; word 10: unused
	; word 11: unused
	; word 12: unused
	; word 13: unused
	; word 0: second gate ID number
	; word 1: ...
	;	...remainder of the gates...
	;
	; WARNING: if you add more entries to this buffer, you must adjust
	;           GATE_PARAMS_SIZE constant.
	; 
	; All values are defined in sample counts - i.e a width of 3 is a width
	; of 3 A/D sample counts.
	;
	; If interface gate is in use, it must always be the first gate.  The
	; section numbers are useful for debugging purposes if nothing else.
	;
	; If interface tracking is off, the start position is based from the
	; initial pulse.  Since the FPGA delays the start of sample collection
	; based upon the "hardware delay" set by the host, this delay must
	; be accounted for by subtracting it from each gate start so they
	; are correct in relation the beginning of the data set.  The adjusted
	; values are updated with each pulsing of the transducers to account
	; for any changes which may have been made to the variable hardwareDelay
	; by the host.  Note that the hardwareDelay value should match the
	; value set in the FPGA.
	;
	; If interface tracking is on, the start position is based from the
	; first point where the signal rises above the interface gate.  After
	; each transducer pulse, the interface crossing is detected and added
	; to each gate's location and stored in each gate's "adjusted" variable.
	;
	; Bit assignments for the Gate Function flags:
	;
	; bit 0 :	0 = gate is inactive
	; 			1 = gate is active
	; bit 1 :	0 = no secondary flag
	;			1 = secondary flag if signal does NOT exceed gate
	;				(useful for loss of interface or backwall detection)
	; bit 2 :	0 = flag if signal greater than gate (max gate)
	; 			1 = flag if signal less than gate (min gate)
	;			 (see Caution 1 below)
	; bit 3:	0 = not used for wall measurement
	;			1 = used as first gate for wall measurement
	; bit 4:	0 = not used for wall measurement
	;			1 = used as second gate for wall measurement
	; bit 5:	0 = do not search for signal crossing
	;			1 = search for signal crossing
	;				(must be set if gate is interface or bits 3 or 4 set)
	; bit 6:	0 = gate does not use interface tracking
	;			1 = gate uses interface tracking
	;				(interface gate itself must NOT use tracking)
	; bit 7:	0 = do not search for a peak
	;			1 = search for a peak
	; bit 8:	0 = this is not the interface gate
	;			1 = this is the interface gate
	; bit 9: 	unused
	; bit 10:	unused
	; bit 11:	unused
	; bit 12:	unused
	; bit 13:	unused
	; bit 14:	lsb - gate data averaging buffer size
	; bit 15:	msb - gate data averaging buffer size
	;
	; Caution 1: the max/min gate bit 2 matches the bit 2 position in the gate results
	;            buffer flags so that the bit can easily be copied from the former to
	;			 the latter before sending peak data to the host
	;			 DO NOT MOVE unless all other code here and in the host is matched.
	;

	.bss	gateBuffer, GATE_PARAMS_SIZE * 10;

	; Gate Results Buffer Notes
	;
	; The gateResultsBuffer section is for storing the data collected
	; for 10 gates.
	; Each gate is defined by 12 words each:
	;
	; word 0: first gate ID number
	; word 1: gate result flags (see below)
	; word 2: not used
	; word 3: level exceeded count
	; word 4: level not exceeded count
	; word 5: signal before exceeding
	; word 6: signal before exceeding buffer address
	; word 7: signal after exceeding
	; word 8: signal after exceeding buffer address
	; word 9: peak in the gate (max for a max gate, min for a min)
	; word 10: peak buffer address
	; word 11: peak tracking location
	; word 0: second gate ID number
	; word 1: ...
	;	...remainder of the gates...
	;
	; Bit assignments for the Gate Result flags:
	;
	; bit 0 :	0 = no signal exceeded gate more than hitCount times consecutively
	; 			1 = signal exceeded gate more than hitCount times consecutively
	; bit 1 :	0 = signal did not miss gate more than allowed limit
	; 			1 = signal failed to exceed gate more than missCount times consecutively
	; bit 2:	0 = max gate, higher values are worst case
	; 			1 = min gate, lower values are worst case
	;			 (see Caution 2 below)
	;
	; Notes:
	;
	; Bit 0 is to flag if the signal went above the gate.  It is only set if
	; the violation occurred more than hitCount times in a row.  This flag
	; typically catches flaw indications.
	;
	; Bit 1 is to flag if the signal NEVER went above the gate. It is only set
	; if failure to exceed occurred more than missCount times in a row.  This
	; is typically used to detect loss of interface or loss of backwall.
	;
	; Every time the signal does not exceed the gate, hitCount is reset.
	; Every time the signal does exceed the gate, missCount is reset
	;
	; NOTE NOTE NOTE
	;
	; Each core processes every other transducer pulse, so a hitCount of
	; two requires only that violations occur on shots 1 and 3 with the
	; second and forth shot being handled by another DSP.
	;
	; Caution 2: the max/min gate bit 2 matches the bit 2 position in the gate
	;            buffer flags so that the bit can easily be copied from the latter to
	;			 the former before sending peak data to the host
	;			 DO NOT MOVE unless all other code here and in the host is matched.
	;

	.bss	gateResultsBuffer, GATE_RESULTS_SIZE * 10

	; DAC Buffer Notes
	;
	; The dacBuffer section is for storing 10 DAC sections (also called gates).
	; Each section is defined by 9 words each:
	;
	; The DAC sections define the gain multiplier to be applied to each
	; section of the sample data set.  The start positions are handled the
	; same as for the gates.  The sections allow the signal amplitude to
	; be set to different values along the timeline.
	;
	; word 0: first section ID number
	; word 1: section function flags
	; word 2: section start location MSB
	; word 3: section start location LSB
	; word 4: section adjusted start location
	; word 5: section width
	; word 6: section gain
	; word 7: unused
	; word 8: unused
	; word 0: second section ID number
	; word 1: ...
	;	...remainder of the gates...
	;
	; All values are defined in sample counts - i.e a width of 3 is a width
	; of 3 A/D sample counts.
	;
	; This section's operation with Interface Tracking is identical to that
	; of the gates - see "Gate Buffer Notes" above for details.
	;
	; Bit assignments for the Section Function flags:
	;
	; bit 0 :	0 = section is inactive
	; 			1 = section is active
	;

	.bss	dacBuffer, DAC_PARAMS_SIZE * 10

	.bss	stack, 99				; the stack is set up here
	.bss	endOfStack,	1			; code sets SP here on startup
									; NOTE: you can have plenty of stack!
									;   The PIC micro-controller has the limited
									;   stack space, not the C50!


; NOTE: Various buffers are defined at 3000h and up (see above) - be careful about
;       assigning variables past that point.

; end of Variables
;-----------------------------------------------------------------------------

	.data

;-----------------------------------------------------------------------------
; Data - initialized

Data	.word	0000h				; used to mark the first page of data

test	.word	0001h

; end of Variables
;-----------------------------------------------------------------------------

	.text

;-----------------------------------------------------------------------------
; setupDMA
;
; Sets up DMA channels.
; 

setupDMA:

	; setup channel 1 to store data from McBSP1 to a buffer

	call	setupDMA1

	; setup channel 2 to send data from a buffer to McBSP1

	call	setupDMA2

	; setup channel 3 to write data to the program memory

	call	setupDMA3


	; setup registers common to all channels

	stm #0100011000000010b, DMPREC
	
	;0~~~~~~~~~~~~~~~ (FREE) DMA stops on emulation stop
	;~1~~~~~~~~~~~~~~ (IAUTO) set for '5441 to use separate reload registers
	;~~0~~~~~~~~~~~~~ (DPRC[5]) Channel 5 low priority
	;~~~0~~~~~~~~~~~~ (DPRC[4]) Channel 4 low priority
	;~~~~0~~~~~~~~~~~ (DPRC[3]) Channel 3 low priority
	;~~~~~1~~~~~~~~~~ (DPRC[2]) Channel 2 high priority
	;~~~~~~1~~~~~~~~~ (DPRC[1]) Channel 1 high priority
	;~~~~~~~0~~~~~~~~ (DPRC[0]) Channel 0 low priority
	;~~~~~~~~00~~~~~~ (INTOSEL) N/A here as interrupts are disabled
	;~~~~~~~~~~0~~~~~ (DE[5]) Channel 5 disabled
	;~~~~~~~~~~~0~~~~ (DE[4]) Channel 4 disabled
	;~~~~~~~~~~~~0~~~ (DE[3]) Channel 3 disabled (enabled when time to send)
	;~~~~~~~~~~~~~0~~ (DE[2]) Channel 2 disabled (enabled when time to send)
	;~~~~~~~~~~~~~~1~ (DE[1]) Channel 1 enabled
	;~~~~~~~~~~~~~~~0 (DE[0]) Channel 0 disabled

	; Note - The basic *54x used a common set of reload registers for all
	; channels. The TMS320VC5441 has separate reload registers for each channel.
	; To enable the use of the separate registers set bit 14 (IAUTO) of DMPREC.

	ret

; end of setupDMA
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setupDMA1 (DMA Channel 1 transfer from McBSP1)
;
; This function prepares DMA Channel 1 to transfer data received on the
; McBSP1 serial port to a circular buffer.
;
; Transfer mode: ABU non-decrement
; Source Address: McBSP1 receive register (DRR11)
; Destination buffer: SERIAL_PORT_RCV_BUFFER (in data space)
; Sync event: McBSP1 receive event
; Channel: DMA channel #1
;

setupDMA1:

	stm		DMSRC1, DMSA			;set source address to DRR11
	stm		DRR11, DMSDN

	stm		DMDST1, DMSA			;set destination address to buffer
	stm		#SERIAL_PORT_RCV_BUFFER, DMSDN

	stm		DMCTR1, DMSA			;set buffer size
	stm		#SERIAL_PORT_RCV_BUFSIZE, DMSDN

	stm		DMSFC1, DMSA	
	stm		#0101000000000000b, DMSDN
	
	;0101~~~~~~~~~~~~ (DSYN) McBSP1 receive sync event
	;~~~~0~~~~~~~~~~~ (DBLW) Single-word mode
	;~~~~~000~~~~~~~~ Reserved
	;~~~~~~~~00000000 (Frame Count) Frame count is not relevant in ABU mode

	stm		DMMCR1, DMSA
	stm		#0001000001001101b, DMSDN

	;0~~~~~~~~~~~~~~~ (AUTOINIT) Autoinitialization disabled
	;~0~~~~~~~~~~~~~~ (DINM) DMA Interrupts disabled
	;~~0~~~~~~~~~~~~~ (IMOD) Interrupt at full buffer
	;~~~1~~~~~~~~~~~~ (CTMOD) ABU (circular buffer) mode
	;~~~~0~~~~~~~~~~~ Reserved
	;~~~~~000~~~~~~~~ (SIND) No modify on source address (DRR11)
	;~~~~~~~~01~~~~~~ (DMS) Source in data space
	;~~~~~~~~~~0~~~~~ Reserved
	;~~~~~~~~~~~011~~ (DIND) Post increment destination address with DMIDX0 *note
	;~~~~~~~~~~~~~~01 (DMD) Destination in data space

	; *note - the basic *54x used DMIXD0 to specify the increment for ALL channels
	; the TMS320VC5441 has a separate increment register for each channel - to
	; enable the use of the separate increments set bit 14 (IAUTO) of DMPREC

	stm		DMIDX0, DMSA			;set element address index to +1
	stm		#0001h, DMSDN

	.newblock						; allow re-use of $ variables

; setupDMA1 (DMA Channel 1 transfer from McBSP1)
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setupDMA2 (DMA Channel 2 transfer to McBSP1)
;
; This function prepares DMA Channel 2 to transfer data from a buffer to the
; McBSP1 serial port.
;
; Transfer mode: Multiframe mode
; Source Address: SERIAL_PORT_XMT_BUFFER (data space)
; Destination buffer: McBSP1 transmit register (DXR11)
; Sync event: free running
; Channel: DMA channel #2
;

setupDMA2:

	stm		DMSRC2, DMSA			;set source address to buffer
	stm		#SERIAL_PORT_XMT_BUFFER, DMSDN

	stm		DMDST2, DMSA			;set destination address to DXR11
	stm		DXR11, DMSDN

	stm		DMCTR2, DMSA			;set element transfer count (this gets
	stm		#0h, DMSDN				; adjusted for each type of packet)

	stm		DMSFC2, DMSA	
	stm		#0110000000000000b, DMSDN
	
	;0110~~~~~~~~~~~~ (DSYN) McBSP1 transmit sync event
	;~~~~0~~~~~~~~~~~ (DBLW) Single-word mode
	;~~~~~000~~~~~~~~ Reserved
	;~~~~~~~~00000000 (Frame Count) 1 frame (desired count - 1)

	stm		DMMCR2, DMSA
	stm		#0000000101000001b, DMSDN

	;0~~~~~~~~~~~~~~~ (AUTOINIT) Autoinitialization disabled - *see note below
	;~0~~~~~~~~~~~~~~ (DINM) DMA Interrupts disabled
	;~~0~~~~~~~~~~~~~ (IMOD) Interrupt at full buffer
	;~~~0~~~~~~~~~~~~ (CTMOD) Multiframe mode
	;~~~~0~~~~~~~~~~~ Reserved
	;~~~~~001~~~~~~~~ (SIND) post increment source address after each transfer
	;~~~~~~~~01~~~~~~ (DMS) Source in data space
	;~~~~~~~~~~0~~~~~ Reserved
	;~~~~~~~~~~~000~~ (DIND)  No modify on destination address (DXR11)
	;~~~~~~~~~~~~~~01 (DMD) Destination in data space


	; Note regarding Autoinitialization
	;  AutoInit cannot be used to just reload the registers as it also restarts
	;  another transfer.  The TI manual only says that it reloads registers.
	;  The DE bit will transition briefly at the end of the block, but this
	;  can be hard to catch.  When AutoInit is off, the DMA sets the DE (disable)
	;  bit off at the end of the block transfer and ceases operation.

	.newblock						; allow re-use of $ variables

; setupDMA2 (DMA Channel 2 transfer to McBSP1)
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setupDMA3 (DMA Channel 3 write to Program Memory)
;
; This function prepares DMA Channel 3 to write to the program memory.
; While the '54x has several instructions for writing to program memory,
; they cannot be used to write to shared program memory on the '5441.
; Only the DMA can write to shared memory.
;
; Transfer mode: Multiframe mode
; Source Address: dma3Source variable
; Destination buffer: various program memory address
; Sync event: free running
; Channel: DMA channel #3
;

setupDMA3:

	stm		DMSRC3, DMSA			;set source address to dma3Source
	stm		#dma3Source, DMSDN

	stm		DMDST3, DMSA			;set destination address to 0x7fff
	stm		#0x7fff, DMSDN			; (code should set this as desired
									;  before initiating a transfer)

	stm		DMCTR3, DMSA			;set element transfer count (this
	stm		#0h, DMSDN				; can be adjusted for each transfer,
									; starts with 0 which is 1 element)

	stm		DMSFC3, DMSA	
	stm		#0000000000000000b, DMSDN
	
	;0000~~~~~~~~~~~~ (DSYN) No sync event
	;~~~~0~~~~~~~~~~~ (DBLW) Single-word mode
	;~~~~~000~~~~~~~~ Reserved
	;~~~~~~~~00000000 (Frame Count) 1 frame (desired count - 1)

	stm		DMMCR3, DMSA
	stm		#0000000001000000b, DMSDN

	;0~~~~~~~~~~~~~~~ (AUTOINIT) Autoinitialization disabled - *see note below
	;~0~~~~~~~~~~~~~~ (DINM) DMA Interrupts disabled
	;~~0~~~~~~~~~~~~~ (IMOD) Interrupt at full buffer
	;~~~0~~~~~~~~~~~~ (CTMOD) Multiframe mode
	;~~~~0~~~~~~~~~~~ Reserved
	;~~~~~000~~~~~~~~ (SIND) No modify on source address
	;~~~~~~~~01~~~~~~ (DMS) Source in data space
	;~~~~~~~~~~0~~~~~ Reserved
	;~~~~~~~~~~~000~~ (DIND) No modify on destination address
	;~~~~~~~~~~~~~~00 (DMD) Destination in program space


	; set all source and destination data and memory page pointers
	; to zero in case transfers are made in both directions

	stm		DMSRCP, DMSA	; DMA source program memory page 0
	stm		#0, DMSDN		; (common to all channels)

	stm		DMDSTP, DMSA	; DMA destination program memory page 0
	stm		#0, DMSDN		; (common to all channels)

	stm		DMSRCDP3, DMSA	; DMA source data memory page 0
	stm		#0, DMSDN		; (applies only to this channel)

	stm		DMDSTDP3, DMSA	; DMA destination data memory page 0
	stm		#0, DMSDN		; (applies only to this channel)


	; Note regarding Autoinitialization
	;  AutoInit cannot be used to just reload the registers as it also restarts
	;  another transfer.  The TI manual only says that it reloads registers.
	;  The DE bit will transition briefly at the end of the block, but this
	;  can be hard to catch.  When AutoInit is off, the DMA sets the DE (disable)
	;  bit off at the end of the block transfer and ceases operation.

	.newblock						; allow re-use of $ variables

; setupDMA3 (DMA Channel 3 write to Program Memory)
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setupSerialPort (McBSP1)
;
; This function prepares the McBSP1 serial port for use.
;

setupSerialPort:

; set up serial port using the following registers:
;
; Serial Port Control Register 1 (SPCR1)
; Serial Port Control Register 2 (SPCR2)
; Pin Control Register (PCR)
;  


; The McBSP registers are not directly accessible.  They are reached
; using sub-addressing - the sub-address of the desired register is
; first stored in SPSA[0-2] and the register is read or written via
; SPSD[0-2] where [0-2] specifies McBSP0, McBSP1, or McBSP2


; Serial Port Control Register 1 (SPCR1)
;
; bit 	15	  = 0 		: RW - Digital loop back mode disabled
; bits	14-13 = 00		: RW - Right justify and zero-fill MSBs in DRR[1,2]
; bits	12-11 = 00		: RW - Clock stop mode is disabled
; bits  10-8  = 000		: R  - reserved
; bit	7	  = 0		: RW - DX enabler is off (delays hi-z to on time for 1st bit)
; bit	6	  = 0		: RW - A-bis mode is disabled
; bit	5-4	  = 00		: RW - RINT interrupt driven by RRDY (end of word)
; bit	3	  = ?		: RW - Rcv sync error flag - write a 0 to clear it
; bit	2	  = ?		: R  - Overrun error flag, possible data loss
; bit	1	  = ?		: R  - Data ready to be read from DDR flag = 1
; bit 	0	  = 0		: RW - Port receiver in reset = 0
;

	stm		#SPCR1, SPSA1			; point subaddressing register
	stm		#00h, SPSD1				; store value in the desired register
	
; Serial Port Control Register 2 (SPCR2)	
;
; bits 	15-10 = 000000	: R  - Reserved
; bit 	9	  = 0		: RW - Free run disabled (used by emulator tester)
; bit 	8	  = 0		: RW - SOFT mode disabled (used by emulator tester)
; bit	7	  = 0		: RW - Frame sync not generated internally so disable
; bit	6	  = 0		: RW - Sample rate generator not used so disable
; bits	5-4	  = 00		: RW - XINT interrupt driven by XRDY (end of word)
; bit	3	  = ?		: RW - Xmt sync error flag - write a 0 to clear it
; bit	2	  = ?		: R  - Transmit shift register is empty (underrun)
; bit	1	  = ?		: R  - Transmitter is ready for new data = 1
; bit	0	  = 0		: WR - Port transmitter in reset = 0
;
; Xmt and Rcv clocks and Frame signals are generated externally so FRST (bit 7)
; and GRST (bit 6) are set 0 to keep them in reset and disabled.  On page 2-24
; of the data manual, a GRST flag is described as being in SRGR2 - there is
; no such flag in that register and apparently the GRST flag in the SPCR2 register
; is what was intended.  The statement "If you want to reset the sample rate
; generator when neither the transmitter nor the receiver is fed by..." is
; misleading - it means that you can disable it by putting it in reset.  Otherwise,
; why would you need to reset it if it is not being used?
;

	stm		#SPCR2, SPSA1			; point subaddressing register
	stm		#00h, SPSD1				; store value in the desired register

; Pin Control Register (PCR)
;
; bits 	15-14 = 00		: R  - Reserved
; bit 	13	  = 0		: RW - DX, FSX, CLKX used for serial port and not I/O
; bit	12	  = 0		: RW - DR, FSR, CLKR, CLKS used for serial port and not I/O
; bit	11	  = 0		: RW - Xmt Frame Sync driven by external source
; bit	10	  = 0		: RW - Rcv Frame Sync driven by external source
; bit	9	  = 0		: RW - Xmt Clock driven by external source
; bit	8	  = 0		: RW - Rcv Clock driven by external source
; bit	7	  = 0		: R  - Reserved
; bit	6	  = ?		: R  - CLKS pin status when used as an input
; bit	5	  = ?		: R  - DX pin status when used as an input
; bit	4	  = ?		: R  - DR pin status when used as an input
; bit	3	  = 0		: RW - Xmt Frame Sync pulse is active high
; bit	2	  = 0		: RW - Rcv Frame Sync pulse is active high
; bit	1	  = 0		: RW - Xmt data output on rising edge of CLKX
; bit	0	  = 0		: RW - Rcv data sampled on falling edge of CLKR
;

	stm		#PCR, SPSA1				; point subaddressing register
	stm		#00h, SPSD1				; store value in the desired register


; set up receiver using the following registers:
;
; Receive Control Register 1 (RCR1)
; Receive Control Register 2 (RCR2)

; Receive Control Register 1 (RCR1)
;
; bit	15	  = 0		: R  - Reserved
; bits 	14-8  = 0000000	: RW - Rcv frame 1 length = 1 word
; bits	7-5	  = 000		: RW - Rcv word length = 8 bits
; bits	4-0	  = 00000	: R  - Reserved
;

	stm		#RCR1, SPSA1			; point subaddressing register
	stm		#00h, SPSD1				; store value in the desired register

; Receive Control Register 2 (RCR2)
;
; bit	15	  = 0		: RW - Single phase rcv frame
; bits	14-8  = 0000000	: RW - Rcv frame 2 length = 1 word (not used for single phase)
; bits	7-5	  = 000		: RW - Rcv word length 2 = 8 bits (not used for single phase)
; bits	4-3	  = 00		: RW - No companding, data tranfers MSB first
; bit	2	  = 0		: RW - Rcv frame sync pulses after the first restart transfer
; bits	1-0	  = 00		: RW - First bit transmitted zero clocks after frame sync

	stm		#RCR2, SPSA1			; point subaddressing register
	stm		#00h, SPSD1				; store value in the desired register


; set up transmitter using the following registers:
;
; Transmit Control Register 1 (XCR1)
; Transmit Control Register 2 (XCR2)

; Transmit Control Register 1 (XCR1)
;
; bit	15	  = 0		: R  - Reserved
; bits 	14-8  = 0000000	: RW - Xmt frame 1 length = 1 word
; bits	7-5	  = 000		: RW - Xmt word length = 8 bits
; bits	4-0	  = 00000	: R  - Reserved
;

	stm		#XCR1, SPSA1			; point subaddressing register
	stm		#00h, SPSD1				; store value in the desired register

; Transmit Control Register 2 (XCR2)
;
; bit	15	  = 0		: RW - Single phase xmt frame
; bits	14-8  = 0000000	: RW - Xmt frame 2 length = 1 word (not used for single phase)
; bits	7-5	  = 000		: RW - Xmt word length 2 = 8 bits (not used for single phase)
; bits	4-3	  = 00		: RW - No companding, data received MSB first
; bit	2	  = 0		: RW - Xmt frame sync pulses after the first restart transfer
; bits	1-0	  = 01		: RW - First bit transmitted one clock after frame sync
;
; The first bit is transmitted one cycle after frame sync because the sync is
; generated externally and it arrives too late to place the first bit without
; waiting for the next cycle.
;

	stm		#XCR2, SPSA1			; point subaddressing register
	stm		#01h, SPSD1				; store value in the desired register

		
	stm		#15, AR1				; wait 15 internal cpu clock cycles (10 ns each)
	banz	$, *AR1-				; to delay at least two serial port clock cycles
									; (60 ns each)


; enable receiver by setting SPCR1 bit 0 = 1

	stm		#SPCR1, SPSA1			; point subaddressing register
	stm		#01h, SPSD1				; store value in the desired register


; do NOT enable transmitter until a packet is received addressing to this
; particular DSP core


; prepare serial port reception by setting pointer to beginning of buffer
; and setting flags and counters

	ld		#Variables1, DP
	st		#SERIAL_PORT_RCV_BUFFER, serialPortInBufPtr

	ret

	.newblock						; allow re-use of $ variables

; end of setupSerialPort (McBSP1)
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; checkSerialInReady
;
; Checks to see if the number of bytes or more specified in the A register are
; available in the serial port receive circular buffer.
;
; If the specified number of bytes or more is available, B register will contain
; a value greater than or equal to zero on exit.  Otherwise B will contain a
; value less than zero:
;
; B >= 0 : number of bytes or more specified are available in buffer
; B < 0  ; number of bytes specified is not available
;

checkSerialInReady:

	ld		#Variables1, DP			; point to Variables1 page

	stm		DMDST1, DMSA			; get current buffer pointer
	ldm		DMSDN, B				; (sign is not extended with ldm)
	
	subs	serialPortInBufPtr, B	; subtract the last buffer position
									; processed from the current position
									; to determine the number of bytes
									; read

	bc		$1, BGEQ				; if current pointer > last processed,
									; jump to check against specified qty

	; current pointer < last processed so it has wrapped past end of the
	; circular buffer, more math to calculate number of words

	ldm		DMSDN, B				; reload current buffer pointer
									; (sign is not extended with ldm)

	; add the buffer size to the current pointer to account for the
	; fact that it has wrapped around

	add		#SERIAL_PORT_RCV_BUFSIZE, B

	subs	serialPortInBufPtr, B	; subtract the last buffer position
									; processed from the current position
									; to determine the number of bytes
									; read

$1:	; compare number of bytes in buffer with the specified amount in A

	sub		A, 0, B					; ( B - A << SHIFT ) -> B 
									;  (the TI manual is obfuscated on this one)

	ret

	.newblock						; allow re-use of $ variables

; end of checkSerialInReady
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; readSerialPort
;
; This function processes packets in the serial port receive circular buffer
; The buffer contains data transferred from the McBSP1 serial port by the
; DMA.
;
; All packets are expected to be the same length for the sake of simplicity
; and execution speed.
;
; Not all packets will be addressed to the DSP core - the serial port is
; shared amongst all 4 cores and all cores receive all packets.  Each
; packet has a core identifier to specify the target core.
;
; This function will discard any packets addressed to other cores until
; a packet is reached which is addressed to the proper core.  The function
; will then process this packet and exit. Thus only one packet addressed
; to the core will be processed while all preceding packets addressed to
; other cores will be discarded. 
;
; On program start, serialPortInBufPtr should be set to the start of the buffer.
;
; Since all packets are the same length, data packet size is always the same.
; 
; The packet to DSP format is:
;
; byte0 	= 0xaa
; byte1 	= 0x55
; byte2 	= 0xbb
; byte3 	= 0x66
; byte4 	= DSP Core identifier (1-4 for cores A-B)
; byte5 	= message identifier
; byte6 	= data packet size (does not include bytes 0-5 or checksum byte12)
; byte7 	= data byte 0
; byte8 	= data byte 1
; byte9 	= data byte 2
; byte10 	= data byte 3
; byte11	= data byte 4
; byte12	= data byte 5
; byte13	= data byte 6
; byte14	= data byte 7
; byte15	= data byte 8
; byte16 	= checksum for bytes 4-15
;

readSerialPort:

	ld		#17, A
	call	checkSerialInReady
	rc		BLT						; B < 0 - bytes not ready so exit

; a packet is ready, so process it

; DP already pointed to Variables1 page by checkSerialInReady function	

	ld		serialPortInBufPtr, A 	; get the buffer pointer
	stlm	A, AR3
	nop								; can't use stm to BK right after stlm AR3
									; and must skip two words before using AR3
									; due to pipeline conflicts

	stm 	SERIAL_PORT_RCV_BUFSIZE, BK		; set the size of the circular buffer
	nop										; next word cannot use circular
											; addressing due to pipeline conflicts

; check for valid 0xaa, 0x55, 0xbb, 0x66 header byte sequence

	ldu		*AR3+%, A				; load first byte of packet header
	sub		#0xaa, A				; compare with 0xaa
	bc		reSync, ANEQ			; error - reSync and bail out

	ldu		*AR3+%, A				; load first byte of packet header
	sub		#0x55, A				; compare with 0x55
	bc		reSync, ANEQ			; error - reSync and bail out

	ldu		*AR3+%, A				; load first byte of packet header
	sub		#0xbb, A				; compare with 0xbb
	bc		reSync, ANEQ			; error - reSync and bail out

	ldu		*AR3+%, A				; load first byte of packet header
	sub		#0x66, A				; compare with 0x66
	bc		reSync, ANEQ			; error - reSync and bail out

; check if packet addressed to this core

	ldu		*+AR3(0)%, A			; load core ID from packet
	sub		coreID, A				; compare the address with this core's ID
	bc		$1, AEQ					; process the packet if ID's match

	mar		*+AR3(13)%				; packet ID does not match DSP core ID, skip
									; to the next packet and ignore this one
	ldm		AR3, A					; save the buffer pointer which now points
	stl		A, serialPortInBufPtr	; to next packet
	ret

; verify checksum

$1:	ldu		*AR3+%, A				; reload core ID from packet
									; (this is first byte included in checksum)
	rpt		#11						; repeat k+1 times
	add		*AR3+%, A				; add in all bytes which are part of the
									; checksum, including the checksum byte

	and		#0xff, A				; mask off upper bits
	bc		reSync, ANEQ			; checksum result not zero, toss and reSync	

	ldm		AR3, A					; save the buffer pointer which now points
	stl		A, serialPortInBufPtr	; to next packet


; process the packet

	;enable the serial port transmitter
	;need to do other tasks for at least 15 internal cpu clock cycles
	;(10 ns each) to delay at least two serial port clock cycles (60 ns each)

	stm		#SPCR2, SPSA1			; point subaddressing register
	stm		#01h, SPSD1				; store value in the desired register
									; this enables the transmitter

	orm		#TRANSMITTER_ACTIVE, flags1	; set transmitter active flag

	mar		*+AR3(-12)%				; point back to packet ID

	; NOTE: the various functions are invoked with a branch rather
	; 		than a call so that they do not return to this function
	;		but instead to that which called this function.  This
	;		reduces code because the message ID in the A register is
	;		destroyed by the function being called and extra loading
	;		or branching would be required if execution returned here.

	ld		*AR3+%, A				; load the message ID
	
	sub		#DSP_GET_STATUS_CMD, 0, A, B	; B = A - (command)
	bc		getStatus, BEQ					; do command if B = 0 

	sub		#DSP_SET_FLAGS1, 0, A, B		; same comment as above for
	bc		setFlags1, BEQ					; this entire section

	sub		#DSP_CLEAR_FLAGS1, 0, A, B
	bc		clearFlags1, BEQ

	sub		#DSP_SET_GAIN_CMD, 0, A, B
	bc		setSoftwareGain, BEQ	

	sub		#DSP_GET_ASCAN_BLOCK_CMD, 0, A, B
	bc		getAScanBlock, BEQ				

	sub		#DSP_GET_ASCAN_NEXT_BLOCK_CMD, 0, A, B
	bc		getAScanNextBlock, BEQ				

	sub		#DSP_SET_AD_SAMPLE_SIZE_CMD, 0, A, B
	bc		setADSampleSize, BEQ				

	sub		#DSP_SET_DELAYS, 0, A, B			
	bc		setDelays, BEQ						

	sub		#DSP_SET_ASCAN_RANGE, 0, A, B		
	bc		setAScanScale, BEQ					

	sub		#DSP_SET_GATE, 0, A, B				
	bc		setGate, BEQ						

	sub		#DSP_SET_GATE_FLAGS, 0, A, B		
	bc		setGateFlags, BEQ					

	sub		#DSP_SET_DAC, 0, A, B				
	bc		setDAC, BEQ							

	sub		#DSP_SET_DAC_FLAGS, 0, A, B			
	bc		setDACFlags, BEQ					

	sub		#DSP_SET_HIT_MISS_COUNTS, 0, A, B	
	bc		setHitMissCounts, BEQ				

	sub		#DSP_GET_PEAK_DATA, 0, A, B			
	bc		getPeakData, BEQ					

	sub		#DSP_SET_RECTIFICATION, 0, A, B		
	bc		setRectification, BEQ				

	ret

	.newblock						; allow re-use of $ variables

; end of readSerialPort
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; reSync
;
; Clears bytes from the socket buffer until 0xaa byte reached which signals
; the *possible* start of a new valid packet header or until the buffer is
; empty.
;
; Calling reSync will increment the reSyncCount variable which tracks
; number of reSyncs required.  If a reSync is not due to an error, call
; reSyncNoCount instead.
;
; On Entry:
;
; AR3 should already be loaded with the current serial in buffer pointer
; BK register should already be loaded with SERIAL_PORT_RCV_BUFSIZE
; DMSA should alread be loaded with DMDST1
;
; On Exit:
;
; If a 0xaa byte was found, serialPortInBufPtr will point to it.
; If byte not found, serialPortInBufPtr will be equal to the DMA
;  channel buffer pointer.
;

reSync:

; count number of times a reSync is required

	ld		reSyncCount, A
	add		#1, A
	stl		A, reSyncCount

reSyncNoCount:


$1:

; check if buffer processing pointer is less than DMA pointer
; stop trying to reSync when pointers match which means all available data
; has been scanned

	ldm		DMSDN, B				; get the serial in DMA buffer pointer
									; (sign is not extended with ldm)

	ldm		AR3, A					; get current buffer pointer

	sub		A, 0, B					; ( B - A << SHIFT ) -> B 
									;  (the TI manual is obfuscated on this one)

	bc		$2, BEQ					; pointers are same, nothing to sync

; data is available - scan for 0xaa
	
	ldu		*AR3+%, A				; load next byte
	sub		#0xaa, A				; compare with 0xaa
	bc		$1, ANEQ				; if not 0xaa, keep scanning

	mar		*AR3-%					; move back to point to the 0xaa byte

$2:

	ldm		AR3, A					; save the buffer pointer
	stl		A, serialPortInBufPtr

	ret

	.newblock						; allow re-use of $ variables

; end of reSync
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; sendPacket
;
; Sends a packet via the serial port.  Enables the serial port, sends the
; data in the buffer, disables the port.
;
; On entry, data to be sent should be stored in SERIAL_PORT_XMT_BUFFER 
; starting at array position 5.  The length of the data should be stored
; in the  A register, the message ID should be in the B register.
;
; The length of the data is not sent back in the packet as the message ID
; will indicate this value.
;
; This function will add header information, using packet format of:
;
; byte 0 : 00h
; byte 1 : 99h
; byte 2 : 0aah
; byte 3 : 055h
; byte 4 : DSP Core ID (1-4 for cores A-D)
; byte 5 : Message ID (matches the request message ID from the host)
; byte 6 : First byte of data
; ...
; ...
;

sendPacket:

	ld		#Variables1, DP
	
	stm		#SERIAL_PORT_XMT_BUFFER, AR3	; point to start of out buffer

	stm		DMSRC2, DMSA			;set source address to buffer
	stm		#SERIAL_PORT_XMT_BUFFER, DMSDN

	add		#5, A					; A contains the size of the data in the buffer
	stm		DMCTR2, DMSA			;  adjust for the header bytes in the total
	stlm	A, DMSDN				;  byte count and store the new value
									; in DMA channel 2 element counter
									; (the value stored is one less than the
									;  actual number of bytes to be sent as
									;  required by the DMA)

; a ffh value has already been sent to trigger the DMA transfer
; this must be followed by 00h, 99h to force the FPGA to begin storing the data

	st		#00h, *AR3+				; trigger to FPGA to start storing
	st		#099h, *AR3+			; trigger to FPGA to start storing
	st		#0aah, *AR3+			; all packets start with 0xaa, 0x55
	st		#055h, *AR3+

	ld		coreID, A				; all packets include DSP core
	stl		A, *AR3+

	stl		B, *AR3+			    ; B contains the message ID

; The serial port will have been enabled for some time and preloaded with
; data value of zero - it may send this value a few times as the frame
; sync is generated by the FPGA.  The FPGA ignores all data received until
; the first 0x99 value is encountered. 

; NOTE NOTE NOTE NOTE
;
; According to "TMS320VC5441 Digital Signal Processor Silicon Errata"
; manual SPRZ190B, a problem can occur when enabling a channel which
; can cause another active channel to also be enabled.  If the other
; active channel finishes and clears its DE bit at the same time as
; an ORM instruction is used to enable different channel, the ORM
; can overwrite the cleared bit the other channel.
; Currently, this code does not have a problem because the only other
; active channel is the serial port read DMA which is always active
; anyway since it is in ABU mode.
; If another channel is used in the future, see the above listed manual
; for ways to avoid the issue.

	ld		#00, DP					; point to Memory Mapped Registers
	orm		#04, DMPREC				; use orm to set only the desired bit
	ld		#Variables1, DP

; since the DMA was disabled when the serial port was enabled, the DMA misses
; the trigger to load data - force feed a first transmit byte to get things
; started

; the FPGA looks for a 0xff, 0x00, 0x99 sequence as a signal to begin storing
; packets - the leading 0xff is transmitted here while the remaining two bytes
; of the header are included at the beginning of the packet - sending the 0xff
; here is necessary to start the DMA tranfer
; The 0xff, 0x00, 0x99 header bytes will not be stored by the FPGA.

	ld		#0ffh, A
	stlm	A, DXR11

	ret

	.newblock						; allow re-use of $ variables

; end of sendPacket
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; getAScanBlock
;
; Returns the first block of words of the AScan data buffer.  The words are
; sent as bytes on the serial port, MSB first.  The number of words to be
; transferred is specified in the requesting packet as the block size.
;
; The number of words should be one less than the amount to be transferred:
;   i.e. a value of 49 returns 50 words which is 100 bytes.
;
; Outgoing packet structure:
;
; byte 0 : current aScanScale (the scale of the compressed data)
; byte 1 : MSB of position where interface exceeds the interface gate
; byte 2 : LSB of above
; bytes 3 - 102 : AScan data
;
; The position is in A/D sample count units and is relative to the
; start of the A/D sample buffer - host must take the hardwareDelay
; value into account.
; 
; On exit the current pointer is stored so the subsequent blocks can be
; retrieved using the getAScanNextBlock function.
; The block size is also saved so it can be used by getAScanNextBlock.
;

getAScanBlock:

	ld		#Variables1, DP

	mar		*AR3+%					; skip past the packet size byte
	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte
	stl		A, getAScanBlockSize	; number of data words to return with
									; each packet

;wip mks
; remove this after Rabbit code changed to send block size -- code above
; already in place to retrieve this from the request packet
	ld 		#49, A
	stl		A, getAScanBlockSize
;end wip mks

	stm		#SERIAL_PORT_XMT_BUFFER+6, AR3	; point to first data word after header

	ld		aScanScale, A				; store the current AScan scaling ratio
	stl		A, *AR3+					; in first byte of packet

	stm		#gateResultsBuffer+8, AR2	; point to the entry of gate 0 (the
										; interface gate if it is in use) which holds
										; the buffer address of the point which first
										; exceeded the interface gate
										; if the interface gate is not in use, then
										; this value should be ignored by the host

	ld		#PROCESSED_SAMPLE_BUFFER, B ; start of buffer
	and		#0ffffh, B					; remove sign - pointer is unsigned

	ldu		*AR2+, A					; load the interface crossing position
										; position is relative to the start of the
										;  buffer, so remove this offset
	sub		B, 0, A						; ( A - B << SHIFT ) -> A 
										;  (the TI manual is obfuscated on this one)
	stl		A, -8, *AR3+				; high byte -- store interface crossing position
	stl		A, *AR3+					; low byte	--   in the packet for host

	stm		#ASCAN_BUFFER, AR2			; point to processed data buffer

	ld		getAScanBlockSize, A		; get number of words to transfer
	stlm	A, AR1
																			
$1:
	ld		*AR2+, A				; get next sample
	
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	banz	$1, *AR1-				; loop until all samples transferred
	
	ldm		AR2, A					; save the buffer pointer so it can be used
	stl		A, getAScanBlockPtr		; in subsequent calls to getAScanNextBlock

	ld		getAScanBlockSize, 1, A ; load block word size, shift to multiply by two
									; to calculate number of bytes
	add		#5, A					; one more word (two more bytes) actually
									; transferred so add 2, three more bytes
									; are added to the packet as well so add 3 more

	ld		#DSP_GET_ASCAN_BLOCK_CMD, B	; load message ID into B before calling
		
	b		sendPacket				; send the data in a packet via serial


	.newblock						; allow re-use of $ variables

; end of getAScanBlock
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; getAScanNextBlock
;
; Returns the next block of words of the AScan data buffer.  The words are
; sent as bytes on the serial port, MSB first.
;
; NOTE: getAScanBlock should be called prior to this function.
;
; See notes for getAScanBlock for details on the packet structure.
;
; On exit the current pointer is stored so the subsequent blocks can be
; retrieved by calling this function repeatedly.
;

getAScanNextBlock:

	ld		#Variables1, DP

	stm		#SERIAL_PORT_XMT_BUFFER+6, AR3	; point to first data word after header

	ld		aScanScale, A					; store the current AScan scaling ratio
	stl		A, *AR3+						; in first byte of packet

	stm		#gateResultsBuffer+8, AR2		; point to the entry of gate 0 (the
											; interface gate if it is in use) which holds
											; the buffer address of the point which first
											; exceeded the interface gate
											; if the interface gate is not in use, then
											; this value should be ignored by the host

	ld		#PROCESSED_SAMPLE_BUFFER, B 	; start of buffer
	and		#0ffffh, B						; remove sign - pointer is unsigned

	ldu		*AR2+, A						; load the interface crossing position

											; position is relative to the start of the
											;  buffer, so remove this offset
	sub		B, 0, A							; ( A - B << SHIFT ) -> A 
											;  (the TI manual is obfuscated on this one)
	stl		A, -8, *AR3+					; high byte -- store interface crossing position
	stl		A, *AR3+						; low byte	--   in the packet for host

	ld		getAScanBlockPtr, A 	; get the packet data pointer
	stlm	A, AR2

	ld		getAScanBlockSize, A	; get number of words to transfer
	stlm	A, AR1
																			
$1:
	ld		*AR2+, A				; get next sample

	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	banz	$1, *AR1-				; loop until all samples transferred
	
	ldm		AR2, A					; save the buffer pointer so it can be used
	stl		A, getAScanBlockPtr		; in subsequent calls to getAScanNextBlock

	ld		getAScanBlockSize, 1, A ; load block word size, shift to multiply by two
									; to calculate number of bytes
	add		#5, A					; one more word (two more bytes) actually
									; transferred so add 2, three more bytes
									; are added to the packet as well so add 3 more

	ld		#DSP_GET_ASCAN_NEXT_BLOCK_CMD, B	; load message ID into B before calling
		
	b		sendPacket				; send the data in a packet via serial

	.newblock						; allow re-use of $ variables

; end of getAScanNextBlock
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; sendACK
;
; Sends and acknowledgement packet back to the host.  Part of the packet
; is the low byte of the resync error count so the host can easily track
; the number of reSync errors that have occurred.
;

sendACK:

	ld		#Variables1, DP

	stm		#SERIAL_PORT_XMT_BUFFER+6, AR3	; point to first data word after header

	ld		reSyncCount, A

	and		#0ffh, 0, A, B			; store low byte
	stl		B, *AR3+

	ld		#1, A					; size of data in buffer

	ld		#DSP_ACKNOWLEDGE, B		; load message ID into B before calling
	
	b		sendPacket				; send the data in a packet via serial

	.newblock						; allow re-use of $ variables

; end of sendACK
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; getStatus
;
; Returns the flags1 word via the serial port.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

getStatus:

	ld		#Variables1, DP

	stm		#SERIAL_PORT_XMT_BUFFER+6, AR3	; point to first data word after header

	ld		flags1, A
	
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		#2, A					; size of data in buffer

	ld		#DSP_GET_STATUS_CMD, B	; load message ID into B before calling
	
	b		sendPacket				; send the data in a packet via serial

	.newblock						; allow re-use of $ variables

; end of getStatus
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setFlags1
;
; Allows the host to set one or more bits in the flags1 variable.
;
; Logical OR's the incoming word with the flags1 variable and stores it
; back in the flags1 variable.  Thus any combination of bits can be set by
; the host by setting the corresponding bit to 1 in the incoming word while
; bits set to 0 will not be changed.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

setFlags1:

	mar		*AR3+%					; skip past the packet size byte

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte

	ld		#Variables1, DP

	or		flags1, A				; OR flag set value from host with old flags1

	stl		A, flags1				; store the new flags

	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of setFlags1
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; clearFlags1
;
; Allows the host to clear one or more bits in the flags1 variable.
;
; Logical AND's the incoming word with the flags1 variable and stores it
; back in the flags1 variable.  Thus any combination of bits can be cleared by
; the host by setting the corresponding bit to 0 in the incoming word while
; bits set to 1 will not be changed.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

clearFlags1:

	mar		*AR3+%					; skip past the packet size byte

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte

	ld		#Variables1, DP

	and		flags1, A				; AND flag set value from host with old flags1

	stl		A, flags1				; store the new flags

	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of clearFlags1
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setSoftwareGain
;
; Sets the gain value from data in a packet.  The signal is multiplied by
; this value and then right shifted 9 bytes to divide by 512, so:
;  Gain = softwareGain / 512.  Thus each count of the gain value multiplies
; the signal by 1/512.  To multiply the signal by 1, gain should be 512;
; to multiply by 3, gain should be 512 * 3 (1,536).  To divide the signal
; by 2, gain should be 512 / 2 (256). 
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

setSoftwareGain:

	mar		*AR3+%					; skip past the packet size byte

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte

	ld		#Variables1, DP

	stl		A, softwareGain			; gain multiplier

	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of setSoftwareGain
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setHitMissCounts
;
; Sets hitCount and missCount.  The value hitCount specifies how many
; consecutive times the signal must exceed the gate before it is flagged.
; The value missCount specifies how many consecutive times the signal must
; fail to exceed the gate before it is flagged.
;
; A value of zero or one means one hit or miss will cause a flag. Values
; above that are one to one - 3 = 3 hits, 4 = 4 hits, etc.
;
; Note: At first glance, it would seem that a hit count of zero would always
; trigger setting of the flag, but the code is never reached unless a peak
; exceeds the gate.  Thus, it functions basically the same as a value of 1.
; Thus, the host needs to catch the special case of zero and ignore the
; flag in that case.
;
; NOTE: Each DSP core processes every other shot of its associated
; channel. So a hit count of 1 will actually flag if shot 1 and shot 3
; are consecutive hits, with shots 2 and 4 being handled by another
; core in the same fashion.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

setHitMissCounts:

	ld		#Variables1, DP

	mar		*AR3+%					; skip past the packet size byte

	ld		*AR3+%, A				; load the gate index number
	call	pointToGateInfo			; point AR2 to info for gate in A

	mar		*+AR2(+6)				; skip to the hit count entry

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte

	stl		A, *AR2+				; hitCount

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte

	stl		A, *AR2+				; missCount

	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of setHitMissCounts
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setRectification
;
; Sets the signal rectification to one of the following for the value in
; the first data byte of the packet:
;
; 0 = Positive half and RF (host computer shifts by half screen for RF)
; 1 = Negative half
; 2 = Full
;
; The code is modified to change an instruction in the sample processing
; loop to perform the necessary rectification.  The code modification is
; used instead of a switch in the loop because the loop is very time
; critical.
;
; DMA channel 3 is used to write the opcodes to program memory.
; While the '54x has several instructions for writing to program memory,
; they cannot be used to write to shared program memory on the '5441.
; Only the DMA can write to shared memory.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

setRectification:

	ld		#Variables1, DP

	mar		*AR3+%					; skip past the packet size byte

	ld		*AR3+%, A				; get rectification selection

; choose the appropriate instruction code for the desired rectification

	sub		#POSITIVE_HALF, 0, A, B	; B = A - (command)
	bc		$3, BNEQ				; skip if B != 0 
	st		#0f495h, dma3Source		; opcode for NOP - for Pos Half Wave
	b		$6
	
$3:	sub		#NEGATIVE_HALF, 0, A, B	; B = A - (command)
	bc		$4, BNEQ				; skip if B != 0 
	st		#0f484h, dma3Source		; opcode for NEG A - for Neg Half Wave
	b		$6

$4:	sub		#FULL_WAVE, 0, A, B		; B = A - (command)
	bc		$5, BNEQ				; skip if B != 0 
	st		#0f485h, dma3Source		; opcode for ABS A - for Full Wave
	b		$6

$5:	sub		#RF_WAVE, 0, A, B	; B = A - (command)
	bc		$6, BNEQ				; skip if B != 0 
	st		#0f495h, dma3Source		; opcode for NOP - for RF_WAVE (same as POS_HALF)

									; if opcode is unknown, will default to RF_WAVE
$6:	stm		DMDST3, DMSA			; set destination address to position of first 
	stm		#rect1, DMSDN			; instruction which needs to be changed
									; in the sample processing loop

; start the DMA transfer - see notes below regarding cautions									

	ld		#00, DP

	orm		#08h, DMPREC			; use orm to set only the desired bit
									; orm to memory mapped reg requires DP = 0

$1:	bitf	DMPREC, #08h			; loop until DMA disabled
	bc		$1, TC					; AutoInit is disabled, so DMA clears this
									; enable bit at the end of the block transfer

	stm		DMDST3, DMSA			; set destination address to position of second
	stm		#rect2, DMSDN			; instruction which needs to be changed
									; in the sample processing loop

; start the DMA transfer - see notes below regarding cautions									

	orm		#08h, DMPREC			; use orm to set only the desired bit
									; orm to memory mapped reg requires DP = 0

$2:	bitf	DMPREC, #08h			; loop until DMA disabled
	bc		$2, TC					; AutoInit is disabled, so DMA clears this

	ld		#Variables1, DP

	b		sendACK					; send back an ACK packet

; NOTE NOTE NOTE NOTE
;
; According to "TMS320VC5441 Digital Signal Processor Silicon Errata"
; manual SPRZ190B, a problem can occur when enabling a channel which
; can cause another active channel to also be enabled.  If the other
; active channel finishes and clears its DE bit at the same time as
; an ORM instruction is used to enable different channel, the ORM
; can overwrite the cleared bit the other channel.
; Currently, this code does not have a problem because the only other
; active channel is the serial port read DMA which is always active
; anyway since it is in ABU mode.
; If another channel is used in the future, see the above listed manual
; for ways to avoid the issue.
;
; It is being used here without precautions - the only other DMA channel
; active at the same time is the serial port receiver and it is in ABU
; mode and never gets disabled.
;

	.newblock						; allow re-use of $ variables

; end of setRectification
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setADSampleSize
;
; Sets the size of the data set which will be transferred into RAM via the
; HPI bus by the FPGA.  The FPGA will collect this many samples but will
; actually transfer half as many words because it packs two byte samples
; into each word transferred.  After the DSP unpacks the incoming buffer
; into the working buffer, the working buffer will have adSampleSize number
; of words - the lower byte of each containing one sample.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

setADSampleSize:

	mar		*AR3+%					; skip past the packet size byte

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte

	ld		#Variables1, DP

	stl		A, adSampleSize			; number of samples

	stl		A, -1, adSamplePackedSize	; number of words transferred in by
										; the FPGA - two samples per word so
										; divide by two
	
	; add the size of the packed buffer stored by the FPGA to the start of
	; the buffer to calculate the end of the buffer

	ld		adSamplePackedSize, A
	
	add		#FPGA_AD_SAMPLE_BUFFER, A

	stl		A, fpgaADSampleBufEnd	; end of the buffer where FPGA stores samples

	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of setADSampleSize
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setDelays
;
; Sets the software delay and the hardware delay for the A/D sample set and
; the AScan dataset.
;
; The software delay (aScanDelay) is the number of samples to skip in
; the collected data set before the start of an AScan dataset.  The data
; collection may start earlier than the AScan, so this delay value is
; is necessary to position the AScan. See notes at the top of processAScanFast
; function for more explanation of this delay value.
;
; The hardware delay should match the value set in the FPGA which specifies
; the number of samples to be skipped before recording starts.  This value
; is used in various places in the DSP code to adjust gate locations and
; such so that they reference the start of data collection.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

setDelays:

	ld		#Variables1, DP

	mar		*AR3+%					; skip past the packet size byte

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte

	stl		A, aScanDelay			; number of samples to skip for AScan

	; this delay value can only be positive as ldu does not sign extend
	; the gate positions can be negative, so this delay value must
	; not be greater than positive max integer value

	ldu		*AR3+%, A				; get byte 3
	sftl	A, 8
	adds	*AR3+%, A				; add in byte 2
	sftl	A, 8
	adds	*AR3+%, A				; add in byte 1
	sftl	A, 8
	adds	*AR3+%, A				; add in byte 0

	sth		A, hardwareDelay1		; number of samples skipped by FPGA
	stl		A, hardwareDelay0		; after initial pulse before recording

	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of setDelays
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setAScanScale
;
; Sets the compression scale for the AScan data set.  This number is actually
; a compression ratio that determines the number of samples to be compressed
; into the AScan data set.  For example, a scale of 3 means three samples
; are to be compressed into every data point in the AScan set.
;
; The value aScanSlowBatchSize is also set -- this value sets the number of
; output data points to be processed in each batch by the slow version of
; the aScan processing function.  This number should be small enough so that
; each batch can be processed while handling data from a UT shot without
; overrunning into the following shot.  As this value is the number of output
; (compressed) data points processed, the number of input data points = 
; aScanSlowBatchSize * aScanScale
;
; This function also calls processAScanSlowInit, initializing variables so
; that processAScanSlow can be called.  It is okay to do this even if the
; slow function is currently filling the AScan buffer, in which case the 
; process will be restarted using the new batch size.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;
; See notes at the top of processAScanFast function for more explanation of this
; scaling value.
;

setAScanScale:

	mar		*AR3+%					; skip past the packet size byte

	ld		#Variables1, DP

	ld		*AR3+%, 8, A			; get high byte of scale
	adds	*AR3+%, A				; add in low byte

	stl		A, aScanScale			; number of samples to compress for
									; each AScan data point

	ld		aScanScale, 1, A		; load the compression scale * 2 (see header note 1)
	bc		$1, AEQ					; if ratio is zero, don't adjust

	sub		#1, A					; loop counting always one less

$1:	stl		A, aScanChunk			; used to count input data points

	ld		*AR3+%, 8, A			; get high byte of batch size
	adds	*AR3+%, A				; add in low byte

	stl		A, aScanSlowBatchSize	; number of output samples to process in
									; each batch for the slow processing

	call	processAScanSlowInit	; init variables for slow AScan processing

	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of setAScanScale
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setGate / setDAC
;
; Sets the start location, width, and height of a gate or DAC section.  The
; first byte in the packet specifies the gate/section to be modified (0-9),
; the next two bytes specify the start location (MSB first), the next two
; bytes specify the width (MSB first), and the last two bytes specify the
; gate height/section gain (MSB first).
;
; This function will return an ACK packet.
;
; Interface tracking can be turned on or off for each gate by setting the
; appropriate bit in the gate's function flags - see setGateFlags.
;
; If interface tracking is off, the start location is in relation to the
; initial pulse.
;
; If interface tracking is on, the interface gate still uses absolute
; positioning while the start location of the other gates is in relation
; to the point where the interface signal exceeds the interface gate
; (this is calculated with each pulse).  If an interface gate is being
; used, it must always be the first gate (gate 0).
;
; See "Gate Buffer Notes" and "DAC Buffer Notes" in this source code file
; for more details.
;
; The gate height is in relation to the max signal height - it is an
; absolute value not a percentage.
;
; To set gate or DAC function flags, see setGateFlags / setDACFlags.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

setGate:	; call here to set Gate info

	mar		*AR3+%					; skip past the packet size byte
	ld		*AR3+%, A				; load the gate index number
	call	pointToGateInfo			; point AR2 to info for gate in A

	b		$1

setDAC:		; call here to set DAC info

	mar		*AR3+%					; skip past the packet size byte
	ld		*AR3+%, A				; load the gate index number
	call	pointToDACInfo			; point AR2 to info for DAC in A

$1:

	mar		*AR2+					; skip the function flags

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte
	stl		A, *AR2+				; store the start location MSB

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte
	stl		A, *AR2+				; store the start location LSB

	mar		*AR2+					; skip the adjusted start location

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte
	stl		A, *AR2+				; store the width

	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3+%, A				; add in low byte
	stl		A, *AR2+				; store the height/gain

	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of setGate / setDAC
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setGateFlags / setDACFlags
;
; Sets the function flags for a gate or DAC section.  The first byte in the
; packet specifies the gate/section to be modified (0-9), the next two bytes
; specify the function flags (MSB first).
;
; NOTE: setGate/setDAC should be called first as this function may use
; values stored by those functions.
;
; If the gate is flagged as an interface, wall start, or wall end, the index
; will be saved in a variable appropriate for each type.  If more than one
; gate is flagged as one of the above, then the last gate flagged will
; be stored.
;
; If the gate is flagged as wall start or wall end, the gate level, gate
; info pointer, and gate results pointer will be stored in variables for
; quick access during processing.
;

setGateFlags:		; call here to set Gate function flags

	mar		*AR3+%					; skip past the packet size byte
	ld		*AR3+%, A				; load the gate index number
	call	pointToGateInfo			; point AR2 to info for gate in A

	b		$1

setDACFlags:		; call here to set DAC function flags

	mar		*AR3+%					; skip past the packet size byte
	ld		*AR3+%, A				; load the gate index number
	call	pointToDACInfo			; point AR2 to info for DAC in A

$1:	ld		*AR3+%, 8, A			; get high byte
	adds	*AR3-%, A				; add in low byte
	stl		A, *AR2					; store the function flags

	mar		*AR3-%					; move back to index number
	ld		*AR3, A					; reload the gate index number
									; don't use % (circular buffer) token here as
									; it is not needed if not inc/decrementing

	bitf	*AR2, #GATE_FOR_INTERFACE	; check if this is the interface gate
	bc		$2, NTC

	stl		A, interfaceGateIndex	; store this index to designate the iface gate	

	b		$4

$2:	bitf	*AR2, #GATE_WALL_START	; check if this is the wall start gate
	bc		$3, NTC

	stl		A, wallStartGateIndex	; store this index to designate wall start gate	

	call	pointToGateInfo			; point AR2 to the info for gate index in A	
									;  also stores index in A in scratch1 for a
									;  call to pointToGateResults

	ldm		AR2, A					; store pointer to gate info in variable
	stl		A, wallStartGateInfo

	mar		*+AR2(+5)				; skip to the gate level
	ld		*AR2, A					; store the gate level in variable
	stl		A, wallStartGateLevel

	call	pointToGateResults		; point AR2 to gate results (index in scratch1)

	ldm		AR2, A					; store pointer to gate results in variable
	stl		A, wallStartGateResults

	b		$4

$3: bitf	*AR2, #GATE_WALL_END	; check if this is the wall end gate
	bc		$4, NTC

	stl		A, wallEndGateIndex		; store this index to designate wall end gate	

	call	pointToGateInfo			; point AR2 to the info for gate index in A	
									;  also stores index in A in scratch1 for a
									;  call to pointToGateResults

	ldm		AR2, A					; store pointer to gate info in variable
	stl		A, wallEndGateInfo

	mar		*+AR2(+5)				; skip to the gate level
	ld		*AR2, A					; store the gate level in variable
	stl		A, wallEndGateLevel

	call	pointToGateResults		; point AR2 to gate results (index in scratch1)

	ldm		AR2, A					; store pointer to gate results in variable
	stl		A, wallEndGateResults

	b		$4

$4:	b		sendACK					; send back an ACK packet

	.newblock						; allow re-use of $ variables

; end of setGateFlags / setDACFlags
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; processAScanFast
;
; (see also processAScanSlow)
;
; Prepares an AScan data set in the ASCAN_BUFFER which can then be transmitted
; to the host.
;
; There are two versions of this function - fast and slow.  The desired mode is
; set by a message call. (wip mks -- this is not yet implemented)
;
; The fast version processes the entire buffer at one time and returns the data
; set in large chunks as requested by the host.  This will usually cause
; degradation in performance of the rest of the code such as peak detection
; because the ASCan processing may not finish before the next transducer
; pulse/acquisition cycle. This mode may be used during setup when speed
; of the AScan display is important but the peak detection is less
; important.
;
; The slow version processes a small part of the buffer with each firing and
; data collection from the pulsers.  A small part of the buffer is then
; returned each time the host asks for peak data. This mode does not interfere
; with timing and the peak detection code will be accurate.  This mode is used
; during inspection to provide periodic updates to an AScan display for
; monitoring purposes.  If necessary, the host can use the peak data to add
; data to the AScan buffer to show peaks on the AScan which would normally
; have been missed because the AScan data is collected only periodically
; over time.  Because the buffer is created from different data sets as only
; a portion is processed with each shot, the result is not a perfect copy
; of a single shot, but the end result is a good representation of the data.
;
; There are two delays involved in providing an AScan - the delay set in
; the FPGA to delay the collection of samples and the aScanDelay which
; provides further delay for the beginning of the AScan data set.  The
; system must capture data from the beginning of the earliest gate, so the
; FPGA sample delay cannot be later than that.  The user may wish to view
; an AScan from a later point, so an added delay is specified by a call to
; setAScanDelay.  The true delay for the AScan is the sum of the FPGA
; sample delay and the aScanDelay.
;
; To fit larger sample ranges into the buffer, the data is compressed by
; the scale factor in aScanScale.  If aScanScale = 0 or 1, no compression is
; performed.  If aScanScale = 3, then the data is compressed by 3.  The
; min and max are collected from aScanScale * 2 samples, then the peaks
; are stored in the AScan buffer in the order in which they were found
; in the raw data.  By storing the peaks in the order they occur, the host
; can redraw the data set more accurately.
;
; Note 1:
;
;  The compressed AScan buffer stores both a minimum and maximum peak for each
;  section of compressed data from the raw buffer.  If only one peak was being
;  kept, the aScanScale value could be used as is to scan that number of raw
;  data values to catch the peak and the compression would be proper.  Since
;  two buffer spaces are being used instead, twice as much data must be scanned
;  for those two spaces to get the same compression, thus the aScanScale value
;  is multiplied by two to obtain the proper count value.
;  Recap using scale of 2 as an example:
;	if one peak is stored, that peak represents represents 2 raw data points
;	if two peaks are stored (as used here), those represent 4 raw data points
;
; On entry:
;
; DP should point to Variables1 page.
;

processAScanFast:

	ld		aScanScale, 1, A		; load the compression scale * 2 (see header note 1)
	stlm	A, AR0					; use to reset the raw buffer index in AR1
									; by using:		mar		*AR1-0

	ld		#PROCESSED_SAMPLE_BUFFER, A		; init input buffer pointer
	adds	aScanDelay, A					; skip samples to account for delay
	stlm	A, AR1

	stm		#ASCAN_BUFFER, AR2		;(stm has no pipeline latency problem)

	stm		#399, AR3				; number of samples for transfer - 1

scanSlowEntry:						; function processAScanSlow uses this entry
									; point to process a batch of data points
$1:

	ld		aScanChunk, A			; counts input data points per data output point
	stlm	A, AR4					; use as a counter to catch max peaks
	stlm	A, AR5					; use as a counter to catch min peaks

; scan through the data to be compressed for max peak

$8:	ld		#8000h, A				; prepare to catch max peak

$2:
	ld		*AR1+, B				; get next sample
	max		A						; max of A & B -> A, c=0 if A>B
	bc		$3, NC					; jump if A > B, no new peak	

	ldm		AR1, B					; store location + 1 of new peak
	stl		B, aScanMaxLoc

$3:	banz	$2, *AR4-				; count thru number samples to compress

	stl		A, aScanMax				; store the max peak
	mar		*AR1-0					; jump back to redo samples for min peak

; scan again through the data to be compressed, this time for min peak

	ld		#7fffh, A				; prepare to catch min peak

$4:
	ld		*AR1+, B				; get next sample
	min		A						; min of A & B -> A, c=0 if A<B
	bc		$5, NC					; jump if A < B, no new peak	

	ldm		AR1, B					; store location + 1 of new peak
	stl		B, aScanMinLoc

$5:	banz	$4, *AR5-				; count thru number samples to compress

	stl		A, aScanMin				; store the min peak

; determine which peak was found first

	ld		aScanMaxLoc, A
	ld		aScanMinLoc, B

	min		A						; min location = first peak
	bc		$6, C					; jump if B < A
	
	ld		aScanMax, A				; store max peak first
	ld		aScanMin, B
	b		$7
								
$6:

	ld		aScanMin, A				; store min peak first
	ld		aScanMax, B

$7:

	stl		A, *AR2+				; store peaks in AScan buffer
	stl		B, *AR2+


	banz	$1, *AR3-				; loop until batch is complete

	ret

	.newblock						; allow re-use of $ variables

; end of processAScanFast
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; processAScanSlow
;
; (see also processAScanFast)
; (see also processAScanSlowInit)
;
; Prepares an AScan data set in the ASCAN_BUFFER which can then be transmitted
; to the host.
;
; This function performs the same operation as processAScanFast, but breaks
; the processing into small chunks so small portions can be done with each
; pulse fire/data collection routine.  This allows the AScan buffer to be
; populated without missing a data set -- processAScanFast causes data sets
; to be lost because it takes so much time.
;
; See header notes for processAScanFast for details.  This slower function
; uses variables to store the states of the different pointers and counters
; between processing each batch.  The function processAScanSlowInit should
; be called first to set the pointers and counters up the first time.
; Thereafter, this function will call the init to restart each time the
; data buffer has been entirely processed.
;
; Warning: data may be stored a bit beyond the end of the AScan buffer
; as the total number of points processed is checked after each batch.  The
; number of points processed in each batch may not be an exact multiple.
;
; On entry:
;
; Before the first call, processAScanSlowInit should have been called.
; DP should point to Variables1 page.
;

processAScanSlow:

; load all the variables

	ld		aScanScale, 1, A		; load the compression scale * 2 (see header note 1)
	stlm	A, AR0					; use to reset the raw buffer index in AR1
									; by using:		mar		*AR1-0

	ld		inBufferPASS, A			; load input buffer pointer
	stlm	A, AR1

	ld		outBufferPASS, A		; load output buffer pointer
	stlm	A, AR2

	ld		totalCountPASS, A		; load total output data counter
	stlm	A, AR6

	ld		aScanSlowBatchSize, A	; load number of output points to process in one batch
	stlm	A, AR3

	call	scanSlowEntry			; call the processAScanFast function to use
									; it to process one batch of data points

	banz	$1, *AR6-				; stop when entire output buffer filled
									; Warning: may go a bit past the buffer end
									; because the batches process multiple points
									; between each time the total count is checked.


	b		processAScanSlowInit	; call init again to start over


; save the new state of the variables
; aScanSlowBatchSize in AR3 is not saved as it never changes
; the compression scale is saved even though it does not change
; since it is manipulated (multiplied by 2) by the init

$1:

	ldm		AR1, A				; save input buffer pointer
	stl		A, inBufferPASS

	ldm		AR2, A				; save output buffer pointer
	stl		A, outBufferPASS

	ldm		AR6, A				; load total output data counter
	stl		A, totalCountPASS

	ret

	.newblock						; allow re-use of $ variables

; end of processAScanSlow
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; processAScanSlowInit
;
; (see also processAScanFast)
; (see also processAScanSlowInit)
;
; Initializes variables for processAScanSlow.  Must be called before
; processAScanSlow is called for the first time.  Thereafter, processAScanSlow
; will call this function itself to restart each time the data buffer is
; completely processed.
;
; The variables used all have the anacronym PASS appended to their names.
;
; On entry:
;
; DP should point to Variables1 page.
;

processAScanSlowInit:

	ld		#PROCESSED_SAMPLE_BUFFER, A		; init input buffer pointer
	adds	aScanDelay, A					; skip samples to account for delay
	stl		A, inBufferPASS

	ld		#ASCAN_BUFFER, A		; init output buffer pointer
	stl		A, outBufferPASS

	ld		#399, A					; init total output data counter
	stl		A, totalCountPASS

	ret

	.newblock						; allow re-use of $ variables

; end of processAScanSlowInit
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; calculateGateIntegral
;
; Finds the integral of the data bracketed by the gate which has a level
; above the gate.  Anything below the gate level is ignored.
;
; NOTE: This should work for all modes -- +Half, -Half, Full, and RF since
; only data above the gate is processed.  For most purposes, the gate should
; be a "max" gate.
;
; On entry, AR2 should point to the flags entry for the gate.  Variable
; scratch1 should contain the gate's index.
; 
; The integral is stored in the gate's peak results entry in gateResultBuffer.
;
; NOTE: If the signal equals the gate level, it is considered to exceed it.
;
; If the gate is a "max" gate, the signal must go higher than the gate's
; height.  If it is a "min" gate, the signal must go lower.
;
; If the peak is higher than the gate level, the gate's hit counter value
; in the results buffer is incremented.  If the hit count value reaches
; the hitCount setting, the appropriate bit is set in the gate's results
; flags.
;
; NOTE: The "adjusted start location" for the gate should be calculated
; before calling this function.
;

calculateGateIntegral:

; if you include the next line, also include the branch to copyToAveragingBuffer at the end of this function
;	call	averageGate				; average the sample set with the previous
									; set(s) -- up to four sets can be averaged

									; AR2 already point to the gate's paramaters
	mar		*AR2+					; skip the flags
	mar		*AR2+					; skip the MSB raw start location
	mar		*AR2+					; skip the LSB

	ldu		*AR2+, A				; set AR3 to adjusted start location of the gate
	stlm	A, AR3
	stlm	A, AR4					; AR4 is passed to storeGatePeakResult as the
									; buffer location of the peak -- not useful for
									; the integral so set to the start of the gate

	ld		*AR2, A					; Set block repeat counter to the gate width.
	add		*AR2, A					; Value in param list is 1/3 the true width.
	add		*AR2+, A				; Add three times to get true width.
									; There may be slight round off error here,
									; but shouldn't have significant effect.

	rc		AEQ						; if the gate width is zero, don't attempt
									; to calculate integral

	sub		#1, A					; Subtract 1 to account for loop behavior.
	stlm	A, BRC

	ld		*AR2+, A				; load the gate level
	stl		A, scratch4				; store for quick use

	mar		*+AR2(-6)				; point back to gate function flags

	ld 		#0h, A					; zero A in preparation for summing

; same integration code used for any signal mode, +half, -half, Full, RF
; the result will vary depending on the mode

	rptb	$3


; load each data point and subtract the gate level to shift it down,
; values which then fall below zero will be ignored -- this acts as a
; threshold at the gate level

	ld		*AR3+, B				; load each data point

;debug mks
	ld		#7fffh, B
;end debug mks

	sub		scratch4, B				; subtract an offset (use the
									; gate level)

	nop								; pipeline protection for xc
	nop

	xc		1, BGT
	add		B, A					; sum each data point only if
									; it is greater than zero
		
$3:	nop
	
	sfta	A, -2					; scale down the result
		
	call	storeGatePeakResult		; store the result

;debug mks	ret

;	b		copyToAveragingBuffer	; copy new data to oldest buffer

	.newblock						; allow re-use of $ variables

; end of calculateGateIntegral
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; averageGate
;
; Averages the samples in the gate with the previous sets of data (up to 4
; sets).
;
; The number of sets to average (buffer size) is transferred from the host
; in the upper most two bits of the gate's flags.
;
; The counter tracking which buffer was filled last time is stored in the upper
; most two bits of the gate's ID number.
;
; On entry, AR2 should point to the flags entry for the gate.  Variable
; scratch1 should contain the gate's index.
;
; NOTE: The "adjusted start location" for the gate should be calculated
; before calling this function.
;
; This function does nothing if the averaging buffer size is 0.
;

averageGate:

	ldu		*AR2, A					; get the buffer size from flags -- shift to bit 0
	sfta	A, -14
	rc		AEQ						; do nothing if averaging buffer size is zero
									; zero from host means no averaging

									; AR2 already point to the gate's paramaters
	mar		*AR2+					; skip the flags
	mar		*AR2+					; skip the MSB raw start location
	mar		*AR2+					; skip the LSB

	ldu		*AR2+, A				; set AR3 to adjusted start location of the gate
	stlm	A, AR3
	stl		A, scratch5				; store for later use by copyToAveragingBuffer

	ld		*AR2, A					; Set block repeat counter to the gate width.
	add		*AR2, A					; Value in param list is 1/3 the true width.
	add		*AR2+, A				; Add three times to get true width.
									; There may be slight round off error here,
									; but shouldn't have significant effect.
	sub		#1, A					; Subtract 1 to account for loop behavior.
	stlm	A, BRC


; increment the buffer counter -- if it is equal to the number of
; averaging buffers to be used (specified by host), then reset to
; 1 -- the first time through the counter will be zero so that
; buffer 1 will be used first

	mar		*+AR2(-5)				; move back to the gate's flags
	ldu		*AR2-, B				; load the flags and shift buffer size
									; to average down to bit 0 (this number from host)
	sfta	B, -14

	ldu		*AR2, A					; load gate ID and shift buffer counter to bit 0
	sfta	A, -14
	
	min		A						; is limit or counter bigger?

	bc		$1, C					; if the counter is equal to the max buffer to
									; average, reset counter to 1 

	; increment counter

	ldu		*AR2, A					; get the ID & buffer counter
	add		#04000h, A				; increment counter at bit 14
									
	stl		A, *AR2					; save the ID with new counter

	b		$2
	
$1:	; reset counter to 1

	andm	#03fffh, *AR2			; clear the old counter in ID
	orm		#04000h, *AR2			; set counter bits to 01, point AR2 at flags

$2:

; set up the pointers to each buffer -- they are 2000h apart so they could
; hold the entire 8K data sample set if the gate(s) were that big 

	ldm		AR3, A					; get adjusted gate start location
	add		#2000h, A				; buffer 1
	stlm	A, AR4
	add		#2000h, A				; buffer 2
	stlm	A, AR5
	add		#2000h, A				; buffer 3
	stlm	A, AR6

; the summed data gets stored over the data in the oldest buffer, pointed at by
; AR7 -- the gate's adjusted start entry gets set to this value so the following
; processing functions will operate on that summed data
; after processing, the new data in the 8000h buffer is copied to this oldest
; buffer, overwriting the summed data which will not be needed any more

	ld		#2000h, A				; load spacing between buffers
	stlm	A, T					; preload T for mpya

	ldu		*AR2, A					; load gate ID
	sfta	A, 2					; shift buffer counter up to upper of A for mpya

	mpya	A						; multiply A(bits 32-16) x T -> B

	ldm		AR3, B					; get adjusted gate start location

	add		B, A					; add buffer offset to location in the 8000h buffer
									; where the gate's adjusted start location points at
									; this will now point to the gate's mirror location in
									; the current averaging history buffer

	stlm	A, AR7					; use AR7 to track result buffer
	mar		*+AR2(+4)				; move to adjusted gate start
	stl		A, *AR2					; following processing functions will
									; now work on data at this location

	mar		*+AR2(-3)				; move back to gate flags

	rptb	$4

; add the values from all the buffers together
; if a buffer isn't being used, it will have zeroes and won't affect the
; sum -- this is the simplest way to do it at the time
; all buffers need to be zeroed on program start

	ld		*AR3+, A				; sum each data point from all 4 buffers
	add		*AR4+, A
	add		*AR5+, A		
	add		*AR6+, A
$4:	stl		A, *AR7+
	
;	sfta	A, -2					; scale down the result

	ret
	
	.newblock

; end of averageGate
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; copyToAveragingBuffer
;
; This function copies data for the gate from the buffer at 8000h to the 
; history buffer with specified by the value in the gate's adjusted start
; location parameter. This is done so that the averageGate function can use
; the history buffers to average the signal over time.
;
; On entry, variable scratch1 should contain the gate's index.
;
; Variable scratch5 should contain the gate's adjusted location in the
; 8000h buffer while the gate's adjusted location parameter should contain
; the destination buffer (this is set by the averageGate function).
;
; This data copy is necessary because the 4 buffers are not truly rotated.
; Incoming data is always inserted into the 8000h buffer and then rotated
; to one of the other three.  If the program is instead changed to dump
; incoming data directly into the oldest data buffer then this copy will
; no longer be necessary.
;
; This function does nothing if the averaging buffer size is 0.
;

copyToAveragingBuffer:

	ld		scratch1, A				; get the gate index
	call	pointToGateInfo			; point AR2 to the gate's parameter list

	ldu		*AR2, A					; get the buffer size from flags -- shift to bit 0
	sfta	A, -14
	rc		AEQ						; do nothing if averaging buffer size is zero
									; zero from host means no averaging


	ldu		scratch5, A				; set AR3 to adjusted start location of the gate
	stlm	A, AR3					;  this is the new data in the 8000h buffer
									;  this pointer stored by function averageGate

	mar		*+AR2(+3)				; move to adjusted start location in the gate's
									; parameters -- averageGate function sets this
									; to the buffer to be used next

	ldu		*AR2+, A				; set AR3 to adjusted start location of the gate
	stlm	A, AR4

	ld		*AR2, A					; Set block repeat counter to the gate width.
	add		*AR2, A					; Value in param list is 1/3 the true width.
	add		*AR2+, A				; Add three times to get true width.
									; There may be slight round off error here,
									; but shouldn't have significant effect.
	sub		#1, A					; Subtract 1 to account for loop behavior.
	stlm	A, BRC

	mar		*+AR2(-5)				; move back to gate flags

	rptb	$1						; copy newest data to the oldest buffer
$1:	mvdd	*AR3+, *AR4+

	ret
	
	.newblock

; end of copyToAveragingBuffer
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; findGatePeak
;
; Finds the peak value and its location for the gate entry pointed to by AR2.
; On entry, AR2 should point to the flags entry for the gate.  Variable
; scratch1 should contain the gate's index.
; 
; Preliminary search looks at every 3rd sample to save time. Secondary search
; then finds exact peak. Gate should be greater than 3 samples wide.  Signals
; on the edge may be missed, gate should be slightly wider than necessary to
; avoid problems.
;
; The peak and its buffer location are stored in the gate's results entry
; in gateResultBuffer.
;
; NOTE: If the signal equals the gate level, it is considered to exceed it.
;
; If the gate is a "max" gate, the signal must go higher than the gate's
; height.  If it is a "min" gate, the signal must go lower.
;
; If the peak is higher than the gate level, the gate's hit counter value
; in the results buffer is incremented.  If the hit count value reaches
; the hitCount setting, the appropriate bit is set in the gate's results
; flags.
;
; NOTE: The "adjusted start location" for the gate should be calculated
; before calling this function.
;

findGatePeak:

	stm		#3, AR0					; look at every third sample
									; (AR0 is used to increment sample pointer)

									; AR2 already point to the gate's parameters
	mar		*AR2+					; skip the flags
	mar		*AR2+					; skip the MSB raw start location
	mar		*AR2+					; skip the LSB

	ldu		*AR2+, A				; set AR3 to adjusted start location of the gate
	stlm	A, AR3

	ld		*AR2+, A				; set block repeat counter to the
	sub		#1, A					; gate width / 3, subtract 1 to account
	stlm	A, BRC					; for loop behavior

	mar     *+AR3(2)				; start with third sample

	ld		*AR3+0, A				; Get first sample - preload so
									; reload occurs at end of repeat
									; block - rptb crashes if it points
									; to a bc instruction or similar.
									; This is now the max until replaced.
									; indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation

	mvmm	AR3, AR4				; store the buffer address of the
									; first sample in AR4
									; NOTE: AR4 will actually be loaded
									; with the address + 3 - this must
									; be adjusted back before use

	mar		*+AR2(-5)				; point back to gate function flags

	bitf	*AR2, #GATE_MAX_MIN		; function flags - check gate type

	bc		$2, TC					; look for max if 0 or min if 1

; max gate - look for maximum signal in the gate

	ld		#0x8000, 16, B			; preload B with min value to ignore
									; the first test

	rptb	$3

	max		A						; compare sample with gate height in A
									; the new max will replace old max in A

	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc

	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new
									; max
									; NOTE: AR4 will actually be loaded
									; with the address + 3 - this must
									; be adjusted back before use

$3:	ld		*AR3+0, B				; get next sample 
									; indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation

	
	; look at two skipped samples before and two after peak to find the
	; exact peak - the two after may be slightly passed the gate, but
	; so close as not to matter
	
	mar		*+AR4(-2)				; AR4 points 3 points ahead of the peak
									; adjust to point to one after as this
									; is expected on exit
	mvmm	AR4, AR3				; get buffer address of peak
	mar		*+AR3(-3)				; move back to first of skipped samples
									; just before the found peak

	ld		*AR3+, B				; get sample
	max		A						; compare with peak in A
	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc
	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new max

	ld		*AR3+, B				; get sample
	max		A						; compare with peak in A
	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc
	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new max

	mar		*AR3+					; skip past the peak already found,
									; now do two samples after peak

	ld		*AR3+, B				; get sample
	max		A						; compare with peak in A
	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc
	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new max

	ld		*AR3+, B				; get sample
	max		A						; compare with peak in A
	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc

	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new

	mar		*AR4-					; adjust to point back to peak

	b		storeGatePeakResult

$2:

; min gate - look for minimum signal in the gate

	ld		#0x7fff, 16, B			; preload B with max value to ignore
									; the first test
	rptb	$4

	min		A						; compare sample with gate height in A
									; the new max will replace old max in A

	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc

	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new
									; max
									; NOTE: AR4 will actually be loaded
									; with the address + 3 - this must
									; be adjusted back before use

$4:	ld		*AR3+0, B				; get next sample 
									; indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation
	
	; look at two skipped samples before and two after peak to find the
	; exact peak - the two after may be slightly passed the gate, but
	; so close as not to matter
	
	mar		*+AR4(-2)				; AR4 points 3 points ahead of the peak
									; adjust to point to one after as this
									; is expected on exit
	mvmm	AR4, AR3				; buffer address of peak -> AR3
	mar		*+AR3(-3)				; move back to first of skipped samples
									; just before the found peak

	ld		*AR3+, B				; get sample
	min		A						; compare with peak in A
	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc
	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new
									; max
	ld		*AR3+, B				; get sample
	min		A						; compare with peak in A
	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc
	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new

	mar		*AR3+					; skip past the peak already found,
									; now do two samples after peak

	ld		*AR3+, B				; get sample
	min		A						; compare with peak in A
	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc
	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new
									; max
	ld		*AR3+, B				; get sample
	min		A						; compare with peak in A
	nop								; avoid pipeline conflict with xc
	nop								; two words between test instr and xc
	xc		1, C					; if sample in B > height in A, then
	mvmm	AR3, AR4				; store the buffer address of the new

	mar		*AR4-					; adjust to point back to peak

	b		storeGatePeakResult

	.newblock

; checks first to see if the peak is above the gate level - if so
; increments the hit counter and sets the hit flag if the hitCount
; threshold reached
; then checks to see if new peak is larger than stored peak and
; replaces latter with former if so
; it is done in this order because a peak above the gate still counts
; as a hit even if it is not larger than the stored peak - thus a peak
; above the level will count as the first hit and subsequent hits by
; slightly smaller peaks can still trigger the hit flag

storeGatePeakResult:

	stl		A, scratch2				; save the peak
	ld		*AR2, A					; load the gate flags
	stl		A, scratch3				; store the flags
	ld		*+AR2(+5), A			; load the gate level
	stl		A, scratch4				; store the gate level

	ld		scratch2, B				; load the peak
	ld		scratch4, A				; load the gate level

	bitf	scratch3, #0x02			; function flags - check gate type
	bc		$3, TC					; look for max if 0 or min if 1

; max gate - check if peak greater than gate level

	max		A						; peak >= gate level?
	bc		$4, C					; yes if C set, jump to inc hitCount
	b		handleNoPeakCrossing

$3:	

; min gate - check if peak less than gate level

	min		A						; peak <= gate level?
	bc		$4, C					; yes if C set, jump to inc hitCount
	b		handleNoPeakCrossing

$4:

; since the peak signal exceeded the gate, clear the "not exceeded" count
; and increment the "exceeded" count

	ld		*+AR2(+1), A			; load hit count threshold from gate info
	stl		A, hitCount				; and store it

	call	pointToGateResults		; point to the entry for gate in scratch1
	pshm	AR2						; save the pointer

	mar		*+AR2(3)				; move to "not exceeded" count

	st		#0,*AR2-				; clear the "not exceeded" count

	ld		*AR2, A					; increment the "exceeded" count
	add		#1, A
	stl		A, *AR2
	
	sub		hitCount, A				; see if number of consecutive hits
	bc		$1, ALT					; >= preset limit - skip if not

	st		#0,*AR2-				; clear the "exceeded" count	

	mar		*AR2-					; skip to the gate results flags

	orm		#1, *AR2				; set bit 0 - signal exceeded gate
									; hitCount number of times

$1:	b		checkForNewPeak

; peak signal did not exceed gate, so clear "exceeded count" and
; increment the "not exceeded" count

handleNoPeakCrossing:

	ld		*+AR2(+2), A			; load miss count threshold from gate info
	stl		A, missCount			; and store it

	call	pointToGateResults		; point to the entry for gate in scratch1
	pshm	AR2						; save the pointer

	mar		*+AR2(2)				; move to "exceeded" count

	; since the signal did not exceed the gate, clear the "exceeded" count
	; and increment the "not exceeded" count

	st		#0,*AR2+				; clear the "exceeded" count

	ld		*AR2, A					; increment the "not exceeded" count
	add		#1, A
	stl		A, *AR2
	
	sub		missCount, A			; see if number of consecutive misses
	bc		$2, ALT					; >= preset limit - skip if not

	st		#0,*AR2-				; clear the "not exceeded" count	

	mar		*AR2-					; skip to the gate results flags
	mar		*AR2-

	orm		#2, *AR2				; set bit 1 - signal missed the gate
									; missCount number of times

$2:	b		checkForNewPeak

checkForNewPeak:

; if the new peak is greater/lesser than the stored peak, replace the
; latter with the former

	popm	AR2						; reload the gate results pointer
	nop								; pipeline protection

	mar		*+AR2(8)				; move to stored peak value
	ld		*AR2, A					; load the stored peak
	ld		scratch2, B				; load the new peak

	bitf	scratch3, #0x02			; function flags - check gate type
	bc		$5, TC					; look for max if 0 or min if 1

; max gate - check if peak greater than stored peak

	max		A						; peak >= gate level?
	bc		$6, C					; yes if C set, jump to store
	ld		#0, A					; return 0 - gate was processed
	ret

$5:

; min gate - check if peak less than stored peak

	min		A						; peak <= gate level?
	bc		$6, C					; yes if C set, jump to store
	ld		#0, A					; return 0 - gate was processed
	ret

$6:

	stl		A, *AR2+				; store the new peak in results
	ldm		AR4, A					; get the new peak's buffer address
	stl		A, *AR2+				; store address in results
	ld		trackCount, A			; get the tracking value for new peak
	stl		A, *AR2+				; store tracking value in results
	ld		#0, A					; return 0 - gate was processed

	ret

	.newblock						; allow re-use of $ variables

; end of findGatePeak
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; findGateCrossing
;
; Finds the location where the signal crosses the level for the gate entry
; pointed to by AR2.   AR2 should point to the flags entry for the gate.
; 
; Preliminary search looks at every 3rd sample to save time. Secondary search
; then finds exact location.  Signal must therefore exceed the gate with at
; least three points.  Gate should be greater than 3 samples wide.  Signals
; on the edge may be missed, gate should be slightly wider than necessary to
; avoid problems.
;
; The crossing point and its buffer location and the previous point and
; its buffer location are stored in the gate's results entry in 
; gateResultBuffer.  If A is returned not zero, the data in the results
; entry is undefined and should not be used.
;
; NOTE: If the signal equals the gate level, it is considered to exceed it.
;
; If the gate is a "max" gate, the signal must go higher than the gate's
; height.  If it is a "min" gate, the signal must go lower.
;
; If a crossing is detected, A register returns 0 and the results are
; stored in the gates results entry in the gateResultsBuffer.
; Register A returns -1 if the signal never exceeded the gate.
;
; NOTE: The "adjusted start location" for the gate should be calculated
; before calling this function.
;
; NOTE: This function is similar to findGateCrossingAfterGain.  This
; function does not apply gain to the samples.  The use of the A & B
; registers during the loop are opposite in the two functions because
; the gain version must use A to multiply the gain.  It would be better
; to change this version to use A & B in the same manner.  The gain
; version does jump to part of the code in this version.
;

findGateCrossing:

	stm		#3, AR0					; look at every third sample

	mar		*AR2+					; skip the flags
	mar		*AR2+					; skip the MSB raw start location
	mar		*AR2+					; skip the LSB

	ldu		*AR2+, A				; set AR3 to adjusted start location
	stlm	A, AR3

	ld		*AR2+, A				; set block repeat counter to the
	sub		#1, A					; gate width / 3, subtract 1 to account
	stlm	A, BRC					; for loop behavior

	mar     *+AR3(2)				; start with third sample

	ld		*AR3+0, B				; get first sample - preload so
									; reload occurs at end of repeat
									; block - rptb crashes if it points
									; to a bc instruction or similar
									; indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation

	ld		*AR2+, A				; get the gate height

	mar		*+AR2(-6)				; point back to gate function flags

	bitf	*AR2, #0x02				; function flags - check gate type

	bc		$2, TC					; look for max if 0 or min if 1

	; max gate - look for signal crossing above the gate

	rptb	$3
	max		B						; compare sample with gate height in A
	bc		$4, C					; if sample in B > height in A, exit loop
$3:	ld		*AR3+0, B				; get sample 
									; indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation
	
	b		handleNoCrossing		; handle "signal did not exceed gate"

$4:	; look at skipped samples for earliest to exceed threshold

	mar		*+AR3(-5)				; jump back to first of skipped samples

	ld		*AR3+, B				; get sample
	max		B						; compare with gate height in A
	bc		$5, C	

	ld		*AR3+, B				; get sample
	max		B						; compare with gate height in A
	bc		$5, C	

	;if the skipped samples did not exceed the gate, then AR3 will now
	;point to the sample which did exceed

	b		storeGateCrossingResult

$5:	mar		*AR3-					; move back to point at sample which first
									; exceeded the gate
	b		storeGateCrossingResult

$2:	; min gate - look for signal crossing below the gate

	rptb	$9
	min		B						; compare sample with gate height in A
	bc		$6, C					; if sample in B < height in A, exit loop
$9:	ld		*AR3+0, B				; get sample 
									; indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation


	b		handleNoCrossing		; handle "signal did not exceed gate"

$6:	; look at skipped samples for earliest to exceed threshold

	mar		*+AR3(-5)				; jump back to first of skipped samples

	ld		*AR3+, B				; get sample
	min		B						; compare with gate height in A
	bc		$7, C	

	ld		*AR3+, B				; get sample
	min		B						; compare with gate height in A
	bc		$7, C	

	;if the skipped samples did not exceed the gate, then AR3 will now
	;point to the sample which did exceed

	b		storeGateCrossingResult

$7:
	mar		*AR3-					; move back to point at sample which first
									; exceeded the gate
	b		storeGateCrossingResult

	.newblock

; store the crossing point (first point which equals or exceeds the gate)
; and its buffer location and the previous point and its buffer location


storeGateCrossingResult:

	call	pointToGateResults		; point to the entry for gate in scratch1

	mar		*+AR2(7)				; move to entry for after exceeding address

	ldm		AR3, A					; get location of point which exceeded gate
	stl		A, *AR2-				; store location

	ld		*AR3-, A 				; get point which exceeded gate
	stl		A, *AR2-				; store exceeding point value

	ldm		AR3, A					; get location of previous point
	stl		A, *AR2-				; store location

	ld		*AR3-, A				; get previous point
	stl		A, *AR2-				; store previous point value


	; since the signal exceeded the gate, clear the "not exceeded" count
	; and increment the "exceeded count"

	st		#0,*AR2-				; clear the "not exceeded" count

	ld		*AR2, A					; increment the "exceeded count"
	add		#1, A
	stl		A, *AR2
	
	sub		hitCount, A				; see if number of consecutive hits
	bc		$1, ALT					; >= preset limit - skip if not

	st		#0,*AR2-				; clear the "exceeded" count	

	mar		*AR2-					; skip to the gate results flags

	orm		#HIT_COUNT_MET, *AR2	; set bit 0 - signal exceeded gate
									; hitCount number of times

$1:	ld		#0, A					; return 0 - crossing point found

	orm		#GATE_EXCEEDED, *AR2	; set flag - signal exceeded the gate level

	ret

; signal never exceeded gate, so clear "exceeded count" and increment
; the "not exceeded" count

handleNoCrossing:

	call	pointToGateResults		; point to the entry for gate in scratch1

	mar		*+AR2(2)				; move to "exceeded" count

	; since the signal exceeded the gate, clear the "not exceeded" count
	; and increment the "exceeded count"

	st		#0,*AR2+				; clear the "exceeded" count

	ld		*AR2, A					; increment the "not exceeded count"
	add		#1, A
	stl		A, *AR2
	
	sub		missCount, A			; see if number of consecutive hits
	bc		$2, ALT					; >= preset limit - skip if not

	st		#0,*AR2-				; clear the "not exceeded" count	

	mar		*AR2-					; skip to the gate results flags
	mar		*AR2-

	orm		#MISS_COUNT_MET, *AR2	; set bit 1 - signal did not exceed gate
									; missCount number of times

$2:	ld		#-1, A					; return -1 as no crossing found

	andm	#~GATE_EXCEEDED, *AR2	; clear flag - signal did not exceed the gate level

	ret

	.newblock						; allow re-use of $ variables

; end of findGateCrossing
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; findGateCrossingAfterGain
;
; Finds the location where the signal crosses the level for the gate entry
; pointed to by AR2.   AR2 should point to the flags entry for the gate.
;
; DP should point to Variables1 page.
;
; This function applies softwareGain to each sample before comparing it
; with the gate level.  The sample set is not modified.  This is useful for
; finding the crossing point within the interface gate as the DAC will not
; have been applied when that function must be executed.  The DAC tracks
; the interface crossing, so the crossing point must be found first.
; 
; Preliminary search looks at every 3rd sample to save time. Secondary search
; then finds exact location.  Signal must therefore exceed the gate with at
; least three points.  Gate should be greater than 3 samples wide.  Signals
; on the edge may be missed, gate should be slightly wider than necessary to
; avoid problems.
;
; The crossing point and its buffer location and the previous point and
; its buffer location are stored in the gate's results entry in 
; gateResultBuffer.  If A is returned not zero, the data in the results
; entry is undefined and should not be used.
;
; NOTE: If the signal equals the gate level, it is considered to exceed it.
;
; If the gate is a "max" gate, the signal must go higher than the gate's
; height.  If it is a "min" gate, the signal must go lower.
;
; If a crossing is detected, A register returns 0 and the results are
; stored in the gates results entry in the gateResultsBuffer.
; Register A returns -1 if the signal never exceeded the gate.
;
; NOTE: The "adjusted start location" for the gate should be calculated
; before calling this function.
;
; NOTE: This function is similar to findGateCrossing.  That
; function does not apply gain to the samples.  The use of the A & B
; registers during the loop are opposite in the two functions because
; the gain version must use A to multiply the gain.  This function
; jumps to end code in findGateCrossing.
;

findGateCrossingAfterGain:

	stm		#3, AR0					; look at every third sample

	mar		*AR2+					; skip the flags
	mar		*AR2+					; skip the MSB raw start location
	mar		*AR2+					; skip the LSB

	ldu		*AR2+, A				; set AR3 to adjusted start location
	stlm	A, AR3

	ld		*AR2+, A				; set block repeat counter to the
	sub		#1, A					; gate width / 3, subtract 1 to account
	stlm	A, BRC					; for loop behavior

	ld		softwareGain, A			; get the global gain value
	stlm	A, T					; preload T with the gain multiplier

	mar     *+AR3(2)				; start with third sample

	ld		*AR3+0,16,A				; Get first sample - shift to A Hi
									; for multiply instruction.
									; Preload so reload occurs at end of
									; repeat block - rptb crashes if it points
									; to a bc instruction or similar.
									; Indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation.

	; see notes in header of setSoftwareGain function for details on -9 shift
	mpya	A						; multiply by gain and store in A
	sfta	A,-9					; attenuate

	ld		*AR2+, B				; get the gate height

	mar		*+AR2(-6)				; point back to gate function flags

	bitf	*AR2, #0x02				; function flags - check gate type

	bc		$2, TC					; look for max if 0 or min if 1

	; max gate - look for signal crossing above the gate

	rptb	$3
	max		A						; compare sample with gate height in B
	bc		$4, NC					; if sample in A > height in B, exit loop
	ld		*AR3+0,16,A				; get next sample 
									; indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation

	; see notes in header of setSoftwareGain function for details on -9 shift
	mpya	A						; multiply by gain and store in A
$3:	sfta	A,-9					; attenuate
	
	b		handleNoCrossing		; handle "signal did not exceed gate"

$4:	; look at skipped samples for earliest to exceed threshold

	mar		*+AR3(-5)				; jump back to first of skipped samples

	ld		*AR3+,16,A				; get sample
	mpya	A						; multiply by gain and store in A
	sfta	A,-9					; attenuate
	max		A						; compare with gate height in A
	bc		$5, NC	

	ld		*AR3+,16,A				; get sample
	mpya	A						; multiply by gain and store in A
	sfta	A,-9					; attenuate
	max		A						; compare with gate height in A
	bc		$5, NC	

	;if the skipped samples did not exceed the gate, then AR3 will now
	;point to the sample which did exceed

	b		storeGateCrossingResult

$5:	mar		*AR3-					; move back to point at sample which first
									; exceeded the gate
	b		storeGateCrossingResult

$2:	; min gate - look for signal crossing below the gate

	rptb	$9
	min		A						; compare sample with gate height in A
	bc		$6, NC					; if sample in A < height in B, exit loop
	ld		*AR3+0,16,A				; get next sample 
									; indirect addressing *ARx+0 increments
									; ARx by AR0 after the operation
	; see notes in header of setSoftwareGain function for details on -9 shift
	mpya	A						; multiply by gain and store in A
$9:	sfta	A,-9					; attenuate

	b		handleNoCrossing		; handle "signal did not exceed gate"

$6:	; look at skipped samples for earliest to exceed threshold

	mar		*+AR3(-5)				; jump back to first of skipped samples

	ld		*AR3+,16,A				; get sample
	mpya	A						; multiply by gain and store in A
	sfta	A,-9					; attenuate
	min		A						; compare with gate height in B
	bc		$7, NC	

	ld		*AR3+,16,A				; get sample
	mpya	A						; multiply by gain and store in A
	sfta	A,-9					; attenuate
	min		A						; compare with gate height in B
	bc		$7, NC	

	;if the skipped samples did not exceed the gate, then AR3 will now
	;point to the sample which did exceed

	b		storeGateCrossingResult

$7:
	mar		*AR3-					; move back to point at sample which first
									; exceeded the gate
	b		storeGateCrossingResult

	.newblock

; this code jumps to points storeGateCrossingResult and handleNoCrossing
; in the function findGateCrossing

; end of findGateCrossingAfterGain
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; pointToGateInfo / pointToDACInfo
;
; Uses the gate index stored in register A to point AR2 to the specified
; gate info entry in the gateBuffer or DAC info entry in dacBuffer.
;
; AR2 will point to the flags entry for the gate or DAC.
;

pointToGateInfo:

	ld		#gateBuffer, B			; start of gate info buffer
	stm		#GATE_PARAMS_SIZE, T	; number of words per gate info entry
	b		$1

pointToDACInfo:

	ld		#dacBuffer, B			; start of DAC info buffer
	stm		#DAC_PARAMS_SIZE, T		; number of words per DAC gate info entry

$1:	stl		A, scratch1				; save the gate index number
	mpyu	scratch1, A				; multiply gate number by words per
									; gate to point to gate's info area

	add		B, 0, A					; offset from base of buffer
	stlm	A, AR2					; point AR2 to specified entry

	nop								; pipeline protection
	nop

	mar		*AR2+					; skip past gate ID to point to flags

	ret

	.newblock						; allow re-use of $ variables

; end of pointToGateInfo / pointToDACInfo
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; pointToGateResults
;
; Uses the gate index stored in scratch1 to point AR2 to the specified
; gate results entry in the gateResultsBuffer.
;
; AR2 will point to the flags entry for the gate.
;

pointToGateResults:

	ld		#gateResultsBuffer, B	; start of gate results buffer

									; gate index number already in scratch1
	stm		#12, T					; number of words per gate results entry
	mpyu	scratch1, A				; multiply gate number by words per
									; gate to point to gate's results area

	add		B, 0, A					; offset from base of buffer
	stlm	A, AR2					; point AR2 to specified entry

	nop								; pipeline protection
	nop

	mar		*AR2+					; skip past gate ID to point to flags

	ret

; end of pointToGateResults
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; findInterfaceGateCrossing
;
; Calculates the adjusted gate start location and then calls findGateCrossing
; to find the location where the signal exceeds the gate's level.
;
; On return:
;  crossing found: A = 0 and IFACE_FOUND flag set in processingFlags1
;  no crossing found: A = -1 and IFACE_FOUND flag cleared in processingFlags1
;
; DP should point to Variables1 page.
;
; NOTE: This function will clear bit 6 for the interface gate, the 
; "interface tracking" function as the interface gate itself cannot track
; the interface.
;
; The other flags are not modified, so the host can still disable the gate.
;
; The findGateCrossingAfterGain function is used since it applies the
; softwareGain to the samples.  The interface gate always uses this gain
; as the DAC may be in tracking mode.  For the DAC to track, the interface
; crossing must be found first, so it always uses softwareGain and ignores
; any DAC gain.
;

findInterfaceGateCrossing:

	; set the adjusted start gate value for the interface gate
	; this gate is always absolutely relative to the initial pulse and thereby
	; will be relative to the start of the sample buffer after the adjustment
	; is made

	ld		interfaceGateIndex, A	; load interface gate index
	call	pointToGateInfo			; point AR2 to the info for gate in A

	andm	#0ffbfh, *AR2			; disable "interface tracking" for gate
									; see notes above

	pshm	AR2						; save gate info pointer
	call	setGateAdjustedStart	
	popm	AR2						; restore gate info pointer

	call	findGateCrossingAfterGain	; find where signal crosses above gate

	andm	#~IFACE_FOUND, processingFlags1 ; clear the found flag

	xc		2, AEQ							; if A returned 0 from function call,
											; set the found flag

	orm		#IFACE_FOUND, processingFlags1

	ret

	.newblock						; allow re-use of $ variables

; end of findInterfaceGateCrossing
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setGateAdjustedStart
;
; Calculates the adjusted gate start location for the gate entry pointed
; to by AR2. AR2 should point to the flags entry for the gate.
;
; DP should point to Variables1 page.
;
; If interface tracking is off for the gate, this function will
; adjust the start point so that it is relative to the beginning of the
; sample buffer by subtracting hardwareDelay which represents how many
; data samples are skipped before the FPGA begins recording.
;
; If interface tracking is on for the gate, call the this function will
; adjust the start point so that it is relative to the point where the
; signal first crosses the interface gate.
;
; The crossing function should always be enabled for the interface gate
; as well as any gate which is also enabled as a wall measurement gate.
;
; Interface tracking can be turned on or off for each gate by setting the
; appropriate bit in the gate's function flags.
;

setGateAdjustedStart:

	bitf	*AR2+, #GATE_USES_TRACKING	; check interface tracking flag
										; moves AR2 to gate start entry
	bc		$1, TC						; if TC set, tracking is on

	; interface tracking is off
	; load the hardwareDelay value as the adjustment amount
	; this will set the start relative to the start of the sample buffer

	ld		hardwareDelay1,16,B		; load MSB of hardware delay
	adds	hardwareDelay0,B		; load LSB

	neg		B						; set neg so will be subtracted from start


	; add in the start address of the processed sample buffer so that the
	; start location will be relative to that point

	ld		#PROCESSED_SAMPLE_BUFFER, A ;start of buffer
	and		#0ffffh, A					;remove sign - pointer is unsigned

	add		A, 0, B					; ( B + A << SHIFT ) -> B 
									;  (the TI manual is obfuscated on this one)

	b		$2

$1: ; interface tracking is on

	; store the interface gate crossing point as the adjustment amount
	; this will set the start relative to the interface

	pshd	scratch1				; store index of gate being adjusted
	pshm	AR2						; store address of gate being adjusted

	ld		interfaceGateIndex, A	; load interface gate index
	stl		A, scratch1				; pointToGateResults uses index in scratch1
	call	pointToGateResults		; point AR2 to the results for gate in scratch1

	mar		*+AR2(7)				; point to the entry of gate 0 (the
									; interface gate if in use) which holds the
									; buffer address of the point which first
									; exceeded the interface gate

	ldu		*AR2, B					; get interface crossing buffer location


	popm	AR2						; restore address of gate being adjusted
	popd	scratch1				; restore index and pipeline protect popm AR2

$2:	

	ld		*AR2+,16, A				; load MSB of gate start position
	adds	*AR2+, A				; load LSB

									; add appropriate offset from above
	add		B, 0, A					; ( A + B << SHIFT ) -> A 
									;  (the TI manual is obfuscated on this one)
	stl		A, *AR2					; store the adjusted gate location

	ret

	.newblock						; allow re-use of $ variables

; end of setGateAdjustedStart
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; processWall
;
; Calculate the wall thickness and record the value if it is a new max or min
; peak.
;
; See "Wall Peak Buffer Notes" near the top of this file for details on how
; the wall is processed to improve the resolution.
;
; Note:
; To check for a new peak, only the whole number time span is used.  This is
; is not as accurate as using the whole number plus fraction, but is simpler
; and quicker.  In the future, the whole number plus fraction can be used
; without requiring division by multiplying each numerator by the denominators
; of all other fractions in the equation to normalize.  The whole number can
; be normalized by multiplying it by all other fractions as well. Each normalized
; whole number is added to the normalized numerator of its fraction.  If the
; numerator result was negative, the process still works when adding in the
; normalized whole number.
;

processWall:

	; AR1 => wall peak buffer

	stm		#wallPeakBuffer, AR1

	; to improve the resolution, the actual point in time where the signal
	; crosses the gate level is extrapolated to give a whole number and a
	; fractional value

	; the fractional time distance between the points before crossing the
	; threshold and after crossing is computed - this is done by using the
	; fractional amplitude distance where the gate level lies compared to
	; the amplitude of the points before and after the crossing - the signal
	; can be considered to be nearly linear on the leading edge of the 
	; sinusoid where the gate should be set by the user for best effect, i.e.
	; not near the peak of the signal where it flattens out

	; calculate the whole number time distance between the before crossing points
	; in the start and end gates

	; AR3 => start gate results

	ld		wallStartGateResults, A	; load pointer to wall start gate results
	stlm	A, AR3					;  in AR3

	; AR4 => end gate results

	ld		wallEndGateResults, A	; load pointer to wall end gate results
	stlm	A, AR4					;  in AR4

	nop								; pipeline protection

	mar		*+AR3(5)				; point to buffer location (time position) of
									; crossover point for start gate

	mar		*+AR4(5)				; load buffer location (time position) of point
	ld		*AR4+, A				;  after crossing of end gate (unsigned)
	sub		*AR3+, A  				; subtract start gate crossing from end gate crossing

	stl		A, *AR1+				; save the whole number time distance

	; calculate the denominator of the first gate fraction of a time period
	; the amplitude difference between the before and after crossing points
	; is the denominator of the fraction
	; (the fractional distance where the gate threshold is located between the amplitudes
	;  of the before and after crossing points is used as the approximate fractional
	;  time distance between the points)

	ld		*AR3-, A				; load amplitude of signal after crossing		

	mar		*AR3-					; subtract amplitude of signal before crossing
	sub		*AR3, A					;  this is the denominator

	mar		*AR1+					; point to entry for denominator of start gate crossing
	stl		A, *AR1-				; save the denominator, move back to numerator

	; calculate the numerator for wall start gate
	; subtract the gate level from the signal amplitude before the crossover

	ld		wallStartGateLevel, A	; load wall start gate level
	sub		*AR3, A  				; subtract level of point before crossing

	stl		A, *AR1+				; save the numerator

	; calculate the denominator for wall end gate (see notes for wall start gate)

	ld		*AR4-, A				; load amplitude of signal after crossing

	mar		*AR4-					; subtract amplitude of signal before crossing
	sub		*AR4, A					;  this is the denominator

	mar		*AR1+					; point to entry for denominator of end gate crossing
	mar		*AR1+
	stl		A, *AR1-				; save the denominator, move back to numerator

	; calculate the numerator for wall end gate (see notes for wall start gate)

	ld		wallEndGateLevel, A		; load wall end gate level
	sub		*AR4, A  				; subtract level of point before crossing

	stl		A, *AR1					; save the numerator


	; check for new max peak

	; NOTE: see notes at the top of the function regarding this method and
	;      what could be done to improve it.


	ld		*+AR1(+2), B			; load the old max peak
	mvmm	AR1, AR2				; point AR2 at the max peak variables
	ld		*+AR1(-5), A			; load new value

	max		B						; is new bigger than old?
	bc		$1, C					; no if C set, skip save

	; call to store the new peak whole number and fractional parts

	pshm	AR1
	call	storeNewPeak
	popm	AR1
	nop								; pipeline protection

$1:

	; check for new min peak

	ld		*+AR1(+11), B			; load the old min peak
	mvmm	AR1, AR2				; point AR2 at the max peak variables
	ld		*+AR1(-11), A			; load new value

	min		B						; is new bigger than old?
	rc		C						; no if C set, skip save and return

	; call to store the new peak whole number and fractional parts

storeNewPeak:

	; store the new peak whole number and fractional parts
	; AR1 should point at the new value
	; AR2 should point at the peak variables to be updated

	ld		*AR1+, A 				; whole number
	stl		A, *AR2+				
	ld		*AR1+, A				; numerator start gate
	stl		A, *AR2+				
	ld		*AR1+, A				; denominator start gate
	stl		A, *AR2+				
	ld		*AR1+, A				; numerator end gate
	stl		A, *AR2+				
	ld		*AR1+, A				; denominator end gate
	stl		A, *AR2+				

	ld		trackCount, A			; get the tracking value for new peak
	stl		A, *AR2					; store tracking value in results

	ret

	.newblock						; allow re-use of $ variables

; end of processWall
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; applyGainToEntireSampleSet
;
; Applies value in variable softwareGain (set by host) to the entire sample
; set.  This is for use when the DAC is disabled so that the global gain
; gets applied to the data.  When the DAC is enabled, the DAC gates are
; processed to apply gain to each DAC section of the data.
;
; NOTE: Call this function after processing the interface gate.  That gate
; applies softwareGain as it is processing the data. 
;

applyGainToEntireSampleSet:

	ld		#PROCESSED_SAMPLE_BUFFER, A ;start of buffer
	and		#0ffffh, A					;remove sign - pointer is unsigned
	stlm	A, AR3						; set AR3 & AR4 to buffer start location
	stlm	A, AR4

	ld		adSampleSize, A 		; load size of processed data buffer
	sub		#1, A					; block repeat uses count-1
	stlm	A, BRC					; buffer has two samples per word

	; see notes in header of setSoftwareGain function for details on -9 shift

	ld		#7, ASM					; use shift of -9 for storing (shift=ASM-16)
	ld		softwareGain, A			; get the global gain value
	stlm	A, T					; preload T with the gain multiplier

	ld		*AR3+,16,A				; preload first sample, shifting to
									; upper word of A where mpy expects it
		

	; loop ~ apply DAC section gain to each sample -------------------

	rptb	$3

	mpya	A						; multiply upper sample in A(32-16) by T
									; and store in A

$3:	st		A, *AR4+				; shift right by 9 (using ASM) and store,
	|| ld	*AR3+, A				; load the next sample into upper A
									; where mpy expects it

	; loop end -------------------------------------------------------

	ret

	.newblock						; allow re-use of $ variables

; end of applyGainToEntireSampleSet
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; processDAC
;
; Processes all DAC sections(gates), applying the gain specified for each
; section to the data buffer.
;

processDAC:

	; cannot use block repeat for this AR5 loop because the block
	; repeat is used in some functions called within this loop and there
	; is only one set of block repeat registers - most efficient use is
	; block repeat for the inner loops

	stm		#9, AR5					; loop counter to process all gates

$2:	ldm		AR5, A					; use loop count as gate index

	call	pointToDACInfo			; point AR2 to the info for DAC index in A
									;  also stores index in A in scratch1
									;  for later calls to pointToDACResults

	bitf	*AR2, #GATE_ACTIVE		; function flags - check if gate is active
	bc		$8, NTC					; bit not set, skip this gate

	; adjust each gate to be relative to the start of the sample buffer or the
	; interface crossing if the gate is flagged to track the interface

	pshm	AR2						; save gate info pointer
	call	setGateAdjustedStart
	popm	AR2						; restore gate info pointer
	nop								; pipeline protection

	; apply the gain associated with each DAC section (gate) to the samples

	mar		*AR2+					; skip the flags
	mar		*AR2+					; skip the MSB raw start location
	mar		*AR2+					; skip the LSB

	ldu		*AR2+, A				; set AR3 & AR4 to adjusted start location
	stlm	A, AR3
	stlm	A, AR4

	ld		*AR2+, A				; set block repeat counter to the
	sub		#1, A					; gate width / 3, subtract 1 to account
	stlm	A, BRC					; for loop behavior

	; see notes in header of setSoftwareGain function for details on -9 shift

	ld		#7, ASM					; use shift of -9 for storing (shift=ASM-16)
	ld		*AR2+, A				; get the DAC gate gain
	stlm	A, T					; preload T with the gain multiplier

	ld		*AR3+,16,A				; preload first sample, shifting to
									; upper word of A where mpy expects it
		

	; loop ~ apply DAC section gain to each sample -------------------

	rptb	$3

	mpya	A						; multiply upper sample in A(32-16) by T
									; and store in A

$3:	st		A, *AR4+				; shift right by 9 (using ASM) and store,
	|| ld	*AR3+, A				; load the next sample into upper A
									; where mpy expects it

	; loop end -------------------------------------------------------


$8:	banz	$2,	*AR5-				; decrement DAC gate index pointer

	ret

	.newblock						; allow re-use of $ variables

; end of processDAC
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; processGates
;
; Processes all gates: finding the interface crossing, adjusting all start
; positions, finding signal crossings if enabled, max peaks if enabled.
;
;
; DP should point to Variables1 page.
;
; The interfaceGateIndex, wallStartGateIndex, wallEndGateIndex variables
; are set to ffffh if those gates are not set up.  If they are setup, the
; variables will point to those gates and thus their top bits will be
; zeroed. Checking the top bit can tell if the gate is setup and in use.
;

processGates:

	; if bit 15 of the index is zeroed, interface gate has been set so
	; process it to find the interface crossing point and adjust the
	; other gates if they are using interface tracking
	; (see notes at top of this function for more explanation)

	ld		#0, A					; preload A with interface found flag

	orm		#IFACE_FOUND, processingFlags1
									; preset the interface found flag to true
									; if iface gate is not active, then the
									; flag will be left true so remaining
									; gates will be processed

	bitf	interfaceGateIndex, #8000h
	cc		findInterfaceGateCrossing, NTC

	bitf	flags1, #DAC_ENABLED	; process DAC if it is enabled
	cc		processDAC, TC			;  applies gain for each DAC gate to sample set

	bitf	flags1, #DAC_ENABLED	; apply softwareGain to entire sample set
	cc		applyGainToEntireSampleSet, NTC 		; if DAC not enabled

	; cannot use block repeat for this AR5 loop because the block
	; repeat is used in some functions called within this loop and there
	; is only one set of block repeat registers - most efficient use is
	; block repeat for the inner loops

	stm		#9, AR5					; loop counter to process all gates

$2:	ldm		AR5, A					; use loop count as gate index

	pshm	AR5						; save loop counter as some calls below destroy AR5

	call	pointToGateInfo			; point AR2 to the info for gate index in A
									;  also stores index in A in scratch1
									;  for later calls to pointToGateResults

	bitf	*AR2, #GATE_ACTIVE		; function flags - check if gate is active
	bc		$8, NTC					; bit not set, skip this gate

	; adjust each gate to be relative to the start of the sample buffer or the
	; interface crossing if the gate is flagged to track the interface

	pshm	AR2						; save gate info pointer
	call	setGateAdjustedStart
	popm	AR2						; restore gate info pointer
	nop								; pipeline protection

	bitf	*AR2, #GATE_WALL_START	; find crossing if gate is used for wall start
	bc		$3, NTC

	pshm	AR2						; save gate info pointer
	call	findGateCrossing

	andm	#~WALL_START_FOUND, processingFlags1 ; clear the found flag

	xc		2, AEQ							; if A returned 0 from function call,
											; set the found flag

	orm		#WALL_START_FOUND, processingFlags1

	popm	AR2						; restore gate info pointer
	nop								; pipeline protection

$3:	bitf	*AR2, #GATE_WALL_END	; find crossing if gate is used for wall end
	bc		$4, NTC

	pshm	AR2						; save gate info pointer
	call	findGateCrossing

	andm	#~WALL_END_FOUND, processingFlags1 ; clear the found flag

	xc		2, AEQ							; if A returned 0 from function call,
											; set the found flag

	orm		#WALL_END_FOUND, processingFlags1

	popm	AR2						; restore gate info pointer
	nop								; pipeline protection

$4:	bitf	*AR2, #GATE_FIND_PEAK	; find peak if bit set
	bc		$5, NTC

	pshm	AR2						; save gate info pointer
	call	findGatePeak			; records signal peak in the gate									
	popm	AR2						; restore gate info pointer
	nop								; pipeline protection

$5:	bitf	*AR2, #GATE_INTEGRATE_ABOVE_GATE	; find integral above gate level if bit set
	bc		$6, NTC

	pshm	AR2						; save gate info pointer
	call	calculateGateIntegral	; records integral of signal above gate level
	popm	AR2						; restore gate info pointer
	nop								; pipeline protection

$6:	bitf	*AR2, #GATE_QUENCH_ON_OVERLIMIT	; skip all remaining gates if the integral
	bc		$8, NTC							; above the gate is greater than trigger level
											; WARNING: this section must be after the
											; GATE_INTEGRATE_ABOVE_GATE section as it
											; uses the result from that call

	pshm	AR2						; save gate info pointer


; debug mks -- check for over limit here

	ld		#0, A	;debug mks - remove this after adding quench check

	popm	AR2						; restore gate info pointer
	nop								; pipeline protection

	bc		$8, AEQ					; if A=0, continue processing remaining gates
	popm	AR5						; if A!=0, stop processing gates
	b		$9						;  all remaining gates are ignored

$8:	popm	AR5						; restore loop counter
	nop								; pipeline protection

	banz	$2,	*AR5-				; decrement gate index pointer


	; if either the wall start or end gates have not been set, then exit
	; the entries default to ffffh and the top bit will be set unless those
	; entries have been changed to the index of a wall gate

$9:

	bitf	wallStartGateIndex, #8000h
	rc		TC

	bitf	wallEndGateIndex, #8000h
	rc		TC

	; check to see if the interface was found, the first wall reflection was
	; found, and the second wall reflection was found
	; exit if any were missed - the signal will be ignored

	bitf	processingFlags1, #IFACE_FOUND		; interface check
	rc		NTC

	bitf	processingFlags1, #WALL_START_FOUND	; first reflection check
	rc		NTC

	bitf	processingFlags1, #WALL_END_FOUND	; second reflection check
	rc		NTC

	b		processWall				; calculate the wall thickness

	.newblock						; allow re-use of $ variables

; end of processGates
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; getPeakData
;
; Returns the data peaks collected since the last call to this function and
; resets the peaks in preparation for new ones.
;
; All gates will be checked, the data for any enabled gate will be added
; to the packet.  The packet size will vary depending on the number of enabled
; gates.  The data will be returned in order of the gates, 0 first - 9 last.
;
; On entry, AR3 should be pointing to word 2 (received packet data size) of 
; the received packet.
;

getPeakData:

	ld		#Variables1, DP

	stm		#0, AR2					; tracks number of gate data bytes sent

	stm		#9, BRC					; check for peaks from 10 gates

	stm		#SERIAL_PORT_XMT_BUFFER+6, AR3	; point to first data word after header
	stm		#gateBuffer+1, AR4				; point to gate info buffer (flags of first gate)
	stm		#gateResultsBuffer+1, AR5		; point to gate results (flags of first gate)

; start of peak collection block

	rptb	$1

	bitf	*AR4, #GATE_ACTIVE		; check if gate enabled
	bc		$2, TC					; bit 0 set, send gate's data			

	mar		*+AR4(GATE_PARAMS_SIZE)		; move to parameters for next gate
	mar		*+AR5(GATE_RESULTS_SIZE)	; move to results for next gate
	b		$1

$2:

	mar		*+AR2(8)				; count bytes sent (8 per gate for peak data)

	ldu		*AR4, A					; get the gate flags and mask for the max/min
	and		#GATE_MAX_MIN, A		; flag so it can be transferred to the results
									; flag so the host will know gate type
									; it is transferred repeatedly because the results
									; flag is zeroed each time
			
	adds	*AR5, A					; add the gate type flag to the results flags
	st		#0, *AR5				; zero the results flags

;debug mks -- dont' zero the results flag -- zero specific flags
;such as hit/miss exceeded count
;some flags need to be untouched such as GATE_EXCEEDED used by wall code
;change this as soon as possible

	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	mar		*+AR5(8)				; move to the peak value

	bitf	*AR4, #0x02				; function flags - check gate type
									; need two instructions before xc (pipeline)

	ld		#0x8000, B				; reset peak with min value to search for max

	ld		*AR5, A					; load the peak value

	xc		2, TC					; max or min gate decides reset value
	ld		#0x7fff, B				; reset peak with max value to search for min
									; TC set from bitf above gate is a min

	stl		B, *AR5+				; set peak to appropriate reset value

	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5, A					; load the peak's buffer address
	st		#0, *AR5+				; zero the address

	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5, A					; load the peak's tracking value
	st		#0, *AR5+				; zero the tracking location

	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	mar		*+AR4(GATE_PARAMS_SIZE)	; move to flags of next gate info

	mar		*+AR5(GATE_RESULTS_SIZE-11)	; move to results flags of next gate

$1: nop

; end of peak collection block

	; if both wall start and end gates have not been set, don't send wall data

	bitf	wallStartGateIndex, #8000h
	bc		$3, TC

	bitf	wallEndGateIndex, #8000h
	bc		$3, TC

	; transfer wall max peak data

	stm		#wallPeakBuffer+5, AR5	; point to wall max peak data

	ld		*AR5, A					; load the max peak value
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		#0x8000, B				; reset peak with min value to search for max
	stl		B, *AR5+				; set peak to appropriate reset value

	; transfer the fractional time data - no need to reset to min or max

	ld		*AR5+, A				; numerator start gate
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5+, A				; denominator start gate
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5+, A				; numerator end gate
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5+, A				; denominator end gate
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5+, A				; tracking value
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	; transfer wall min peak data
	
	ld		*AR5, A					; load the min peak value
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		#0x7fff, B				; reset peak with max value to search for min
	stl		B, *AR5+				; set peak to appropriate reset value

	; transfer the fractional time data - no need to reset to min or max

	ld		*AR5+, A				; numerator start gate
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5+, A				; denominator start gate
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5+, A				; numerator end gate
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5+, A				; denominator end gate
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	ld		*AR5+, A				; tracking value
	stl		A, -8, *AR3+			; high byte
	stl		A, *AR3+				; low byte

	mar		*+AR2(24)				; add in bytes for wall data

$3:

	ldm		AR2, A					; size of gate data in buffer

	ld		#DSP_GET_PEAK_DATA, B	; load message ID into B before calling
	
	b		sendPacket				; send the data in a packet via serial

	.newblock						; allow re-use of $ variables

; end of getPeakData
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; processSamples
;
; Processes a new set of A/D samples placed in memory by the FPGA via the
; HPI bus.  The A/D samples are packed - two sample bytes in each word.
; This function unpacks the bytes as it transfers them.
;
; On entry:
;
; A should be zero.
;
; DP should point to Variables1 page.
;
; AR3 should point to the last position of the buffer which will have
; been set to non-zero by the FPGA after writing a sample set.
;
; On exit, the buffer ready flag will be set to 0000h.
;
; The FPGA stores a flag as the last word of the data set.  The most
; significant bit is always 1 while the 15 lower bits specify the frame
; count for the data set.  Since each DSP core processes every other
; data set as the FPGA alternates between two cores for each channel,
; the counter flag will appear to be incremented by two between each
; set.  This counter is compared with the counter from the previous set
; and if there are more than two counts between them it indicates that
; a data set was not properly stored and was missed.  In this case, the
; frameSkipErrors counter will be incremented.
;
; The frame counter will be incremented each time a data set is processed.
; This counter can be retrieved by the host and compared with the number
; of skipped frame errors to determine the error rate.
;

processSamples:

	ld		freeTimeCnt1, 16, B		; load 32 bit free time counter
	adds	freeTimeCnt0, B			;  (used to calculate the amount of free
	sth		B, freeTime1			; store free time value for retrieval by host
	stl		B, freeTime0

	st		#00h, freeTimeCnt1		; zero the free time counter
	st		#00h, freeTimeCnt0

	; A register contains the data set count flag - mask the top bit
	; which is always set to 1 by the FPGA

	and		#7fffh, A
	ld		A, 0, B					; store the flag in the B register

	subs	frameCountFlag, A		; compare new counter with previous one
	bc		$2, AEQ					; if the counters match, no error

	ld		frameSkipErrors, A		; increment the error count
	add		#1, A
	stl		A, frameSkipErrors
	
$2:	add		#2, B					; increment the flag by 2 (each DSP gets every
									;  other frame), 
	and		#7fffh, B				; mask the top bit,
	stl		B, frameCountFlag		; and store it so it will be ready for
									;  the next data set check

	ld		#0, A					; clear the ready flag at the end of the
	stl		A, *AR3					; sample buffer

	; increment the frame counter to track the number of data sets processed

	ld		frameCount1, 16, A		; load 32 bit frame counter
	adds	frameCount0, A
	add		#1, A
	sth		A, frameCount1
	stl		A, frameCount0

	ld		#-8, ASM				; for parallel LD||ST, use shift of 
									; -24 for the save (shift=ASM-16)
									; this shifts the highest byte to the lowest

	; add one to the FPGA buffer to skip past the tracking word

	stm		#FPGA_AD_SAMPLE_BUFFER+1, AR2	; point to start of buffers
	stm		#PROCESSED_SAMPLE_BUFFER, AR3	;(stm has no pipeline latency problem)

	ld		adSamplePackedSize, A 	; load size of FPGA buffer
	sub		#1, A					; block repeat uses count-1
	stlm	A, BRC					; buffer has two samples per word

	; transfer the samples from the FPGA buffer to the processed buffer
	; split each word into one two byte samples (the FPGA packs two samples
	; into each word)

	ld		*AR2,16,A				; preload the first sample pair - shift
									; to AHi to be compatible with code loop

; start of transfer block

	rptb	$1						; transfer all samples

rect1:
	nop								; this nop gets replaced with an instruction
									; which performs the desired rectification
									;  nop for positive half and RF, neg for
									; negative half, abs for full
	
	st		A, *AR3+				; shift right by 24 (using ASM) and store
	|| ld	*AR2+, A				; reload the same pair again to process
									; lower sample - this function shifts the
									; packed samples to A(32-16) as it loads
	
	stl		A, -8, scratch1			; shift down and store the lower sample
									; this will chop off the upper sample and
									; fill the lower bits with zeroes (2)

	ld		scratch1, 16, A			; reload the value into upper A, extending
									; the sign

rect2:
	nop								; this nop gets replaced with an instruction
									; which performs the desired rectification
									;  nop for positive half and RF, neg for
									; negative half, abs for full

$1:
	st		A, *AR3+				; shift right by 24 (using ASM) and store
	|| ld	*AR2, A					; load the next pair without inc of AR2
									; this function shifts the packed samples
									; to A(32-16) as it loads

; end of transfer block

	call	disableSerialTransmitter	; call this often

	bitf	flags1, #GATES_ENABLED		; process gates if they are enabled
	cc		processGates, TC			; also processes the DAC

	call	disableSerialTransmitter	; call this often

	bitf	flags1, #ASCAN_FAST_ENABLED	; process AScan fast if enabled
	cc		processAScanFast, TC		; (this will cause framesets to be skipped
										;  due to the extensive processing required)
										; ONLY use fast or slow version

	bitf	flags1, #ASCAN_SLOW_ENABLED	; process AScan slow if enabled
	cc		processAScanSlow, TC		; (this is safe to use during inspection)
										; ONLY use fast or slow version

	call	disableSerialTransmitter	; call this often

	ret

	.newblock							; allow re-use of $ variables

; end of processSamples
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; setupGatesDACs
;
; Zeroes the gate and DAC section variables and sets the identifier number
; for each.
;

setupGatesDACs:

	ld		#0h, B					; used to zero variables

; set the ID number for each gate and zero its values

	stm		#gateBuffer, AR1		; top of buffer
	ld		#0, A					; start with ID number 0
	stm		#9, BRC					; do 10 gates/sections

	rptb	$2

	stm		#GATE_PARAMS_SIZE-2, AR2 ; zeroes per entry -- 1 less to account for loop
									 ; behavior, 1 more because ID fills a space


	stl		A, *AR1+				; set the ID number
	add		#1, A					; increment to next ID number
$1:	stl		B, *AR1+				; zero the rest of the entry
	banz	$1,	*AR2-

$2: nop								; end of repeat block

; set the ID number for each DAC section and zero its values


	stm		#dacBuffer, AR1			; top of buffer
	ld		#0, A					; start with ID number 0
	stm		#9, BRC					; do 10 DAC sections

	rptb	$4

	stm		#DAC_PARAMS_SIZE-2, AR2	; zeroes per entry -- 1 less to account for loop
									; behavior, 1 more because ID fills a space

	stl		A, *AR1+				; set the ID number
	add		#1, A					; increment to next ID number
$3:	stl		B, *AR1+				; zero the rest of the entry
	banz	$3,	*AR2-

$4: nop								; end of repeat block

; set the ID number for each gate results section and zero its values

	stm		#gateResultsBuffer, AR1	; top of buffer
	ld		#0, A					; start with ID number 0
	stm		#9, BRC					; do 10 gate results entries

	rptb	$6

	stm		#GATE_RESULTS_SIZE-2, AR2	; zeroes per entry -- 1 less to account for loop
										; behavior, 1 more because ID fills a space

	stl		A, *AR1+				; set the ID number
	add		#1, A					; increment to next ID number
$5:	stl		B, *AR1+				; zero the rest of the entry
	banz	$5,	*AR2-

$6: nop								; end of repeat block

	.newblock						; allow re-use of $ variables

; end of setupGatesDACs
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; disableSerialTransmitter
;
; If the DMA has finished transmitting, disable the transmitter so that
; another core can send data on the shared McBSP1 serial port.
;
; NOTE: This function MUST be called as often as possible to release the
;  transmitter for another core as quickly as possible.
;

disableSerialTransmitter:

	ld		#Variables1, DP			; point to Variables1 page

	bitf	flags1, #TRANSMITTER_ACTIVE	; check if transmitter is active
	rc		NTC							; do nothing if inactive

	ld		#00, DP					; must set DP to use bitf
	bitf	DMPREC, #04h			; DMA still enabled, not finished, do nothing
	ld		#Variables1, DP			; point to Variables1 page before return
	rc		TC						; AutoInit is disabled, so DMA clears this
									; enable bit at the end of the block transfer

	; wait until XEMPTY goes low for shift register empty - even when the
	; element count reaches zero for the DMA, the transmitter may still
	; be sending the last value

	stm		#SPCR2, SPSA1			; point subaddressing register
	ld		#00, DP					; must set DP to use bitf
	bitf	SPSD1, #04h				; check XEMPTY (bit 2) to see if all data sent
	ld		#Variables1, DP			; point to Variables1 page before return
	rc		TC						; if bit=1, not empty so loop
	
	stm		#00h, SPSD1				; SPCR2 bit 0 = 0 -> place xmitter in reset

	andm	#~TRANSMITTER_ACTIVE, flags1	; clear the transmitter active flag

	ret

; end of disableSerialTransmitter
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; main
;
; This is the main execution startup code.
;

main:

; The input clock frequency from the FPGA is 8.33 Mhz (120 ns period).  The PLL must be initialized
; to multiply this by 12 to obtain an operating frequency of 100 Mhz (10 ns period).

; 1011011111111111b (b7ffh)
;
; bits  15-12 = 1011 	 : Multiplier = 12 (value + 1) (PLLMUL)
; bit	11	  = 0		 : Integer Multiply Factor (PLLDIV)
; bits 	10-3  = 11111111 : PLL Startup Delay (PLLCOUNT)
; bit 	2	  = 1 		 : PLL On   (PLLON/OFF)
; bit 	1	  = 1 		 : PLL Mode (PLLNDIV)
; bit 	0	  = 1 		 : PLL Mode (STATUS)
;

	ld		#Variables1,DP

	stm		endOfStack, SP			; setup the stack pointer

	;debug mks - for simulation, the next line can be run the first time
	;            to clear the variables and set the index numbers for readability
	;            but then it may be commented out for subsequent program runs
	;            is data is loaded from disk for the Gates & DACs - otherwise
	;            this next call will erase the data each time and it will have to
	;			 be reloaded
	;  !!re-insert the line when not simulating!!

	call	setupGatesDACs			; setup gate and DAC variables

	st		#00h, flags1
	st		#00h, aScanDelay
	st		#512, softwareGain		; default to gain of 1
	st		#1234h, trackCount
	st		#00h, reSyncCount
	st		#00h, hitCount
	st		#00h, missCount
	st		#01h, aScanScale
	st		#00h, frameCount1
	st		#00h, frameCount0
	st		#00h, frameSkipErrors
	st		#00h, frameCountFlag
	st		#00h, processingFlags1
	st		#00h, heartBeat
	st		#00h, freeTimeCnt1
	st		#00h, freeTimeCnt0

	st		#0ffffh, interfaceGateIndex	; default to no gate index set
	st		#0ffffh, wallStartGateIndex	; default to no gate index set
	st		#0ffffh, wallEndGateIndex	; default to no gate index set

	st		#01h, adSamplePackedSize ; The program will attempt to process a
									 ; sample set on startup before data pointers
									 ; and lengths are set - set this value to
									 ; 1 so that only a single data point will
									 ; be processed until real settings are
									 ; transferred by the host.  Related variables
									 ; don't have to be set because only processing
									 ; a single point won't cause any problems.

	stm 	#00h, CLKMD				; must turn off PLL before changing values
									; (not explained very well in manual)
	nop								; give time for system to exit PLL mode
	nop
	nop
	nop

	ld 		#0b7ffh, A
	stlm	A, CLKMD
		
	ldm		CLKMD, A				; store clock mode register so it can be
	stl		A, Variables1			; viewed with the debugger for verification

									; NOTE - only Core A can read or set CLKMD
									; Variables1 will be random for other cores

	ldm		CSIDR, A				; load the DSP Core ID number - this lets
									; the software know which core it is running on

	and		#0fh, A					; lowest 4 bits are the ID (extra bits may be
									; used on future chips with more cores)

	add		#1, A					; the ID is zero based while packets from the
									; host are one based - adjust here to match

	stl		A, coreID				; save the DSP ID core


	call	setupSerialPort			; prepare the McBSP1 serial port for use

	call	setupDMA				; prepare DMA channel(s) for use

	; clear data buffers used for averaging
	ld		#8000h, A
	stlm	A, AR1
	rptz	A, #7fffh
	stl		A, *AR1+

	b		mainLoop				; start main execution loop

; end of main
;-----------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; mainLoop
;
; This is the main execution code loop.
;

mainLoop:

$1:	

	.if 	debug					; see debugCode function for details

	call	debugCode

	.endif

	ld		#Variables1, DP			; point to Variables1 page

	ld		heartBeat, A			; increment the counter so the user can
	add		#1, A					; see that the program is alive when using
	stl		A, heartBeat			; the debugger

	ld		freeTimeCnt1, 16, A		; load 32 bit free time counter
	adds	freeTimeCnt0, A			;  (used to calculate the amount of free
	add		#1, A					;	time between processing each data set
	sth		A, freeTimeCnt1			;	this value is reset for each new data set)
	stl		A, freeTimeCnt0

	; check if FPGA has uploaded a new sample data set - the last value in
	; the buffer will be set to non-zero if so

	ld		fpgaADSampleBufEnd, A 	; get pointer to end of FPGA sample buffer
	stlm	A, AR3
	nop								; pipeline protection
	nop
	ld		*AR3, A					; get the flag set by the FPGA
							
	cc		processSamples, ANEQ	; process the new sample set if flag non-zero

	call	disableSerialTransmitter	; call this often
	
	call	readSerialPort			; read data from the serial port

; check to see if a packet is being sent and disable the serial port transmitter
; when done so that another core can send data on the shared McBSP1

	call	disableSerialTransmitter	; call this often

	b	$1

	.newblock						; allow re-use of $ variables

; end of mainLoop
;-----------------------------------------------------------------------------
