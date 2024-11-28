#Errors observed during code setup:
	1. IllegalArgumentException for get endpoint for account(String)
	2. Naming convension for few packages
	3. Missing Stereo Annotation on email service

#Improvment/Extra work:
	1. While getting sender and reciever details from DB we can pull it in single DB call to avoid multiple DB calls.
	2. For the thread safety we can use optimistic lock based on version control. 
	3. Better Transaction Management to revert changes.
	4. Sending failed transaction emails as well.
	5. We can make notification call Asynchronous as this feature don't have any direct dependancy with transfer money.
	6. Sender and reciver account shouldn't same validation.
	7. We can have indexes at Db end on account No for fast retrival.
	8. For account number we can have proper (9-16 digits) number validation. Current system can accept acc no as 123. Or overall incoming request validation.
	9. Assigning a payment id for every transaction to keep track.
	10. Defining proper constance
