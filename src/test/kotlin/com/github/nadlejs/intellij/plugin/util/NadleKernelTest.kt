package com.github.nadlejs.intellij.plugin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NadleKernelTest {

	// --- parseTaskReference ---

	@Test
	fun `parseTaskReference bare task name`() {
		val ref = NadleKernel.parseTaskReference("build")
		assertEquals("build", ref.taskName)
		assertNull(ref.workspaceInput)
	}

	@Test
	fun `parseTaskReference single workspace qualifier`() {
		val ref = NadleKernel.parseTaskReference("shared:build")
		assertEquals("build", ref.taskName)
		assertEquals("shared", ref.workspaceInput)
	}

	@Test
	fun `parseTaskReference nested workspace qualifier`() {
		val ref = NadleKernel.parseTaskReference("packages:shared:build")
		assertEquals("build", ref.taskName)
		assertEquals("packages:shared", ref.workspaceInput)
	}

	@Test
	fun `parseTaskReference deeply nested qualifier`() {
		val ref = NadleKernel.parseTaskReference("a:b:c:test")
		assertEquals("test", ref.taskName)
		assertEquals("a:b:c", ref.workspaceInput)
	}

	// --- composeTaskIdentifier ---

	@Test
	fun `composeTaskIdentifier with workspace label`() {
		assertEquals(
			"packages:shared:build",
			NadleKernel.composeTaskIdentifier("packages:shared", "build")
		)
	}

	@Test
	fun `composeTaskIdentifier empty label returns bare task name`() {
		assertEquals("build", NadleKernel.composeTaskIdentifier("", "build"))
	}

	@Test
	fun `composeTaskIdentifier simple workspace`() {
		assertEquals(
			"shared:build",
			NadleKernel.composeTaskIdentifier("shared", "build")
		)
	}

	// --- isWorkspaceQualified ---

	@Test
	fun `isWorkspaceQualified returns true for qualified`() {
		assertTrue(NadleKernel.isWorkspaceQualified("shared:build"))
	}

	@Test
	fun `isWorkspaceQualified returns true for deeply qualified`() {
		assertTrue(NadleKernel.isWorkspaceQualified("packages:shared:build"))
	}

	@Test
	fun `isWorkspaceQualified returns false for bare name`() {
		assertFalse(NadleKernel.isWorkspaceQualified("build"))
	}

	// --- deriveWorkspaceId ---

	@Test
	fun `deriveWorkspaceId dot returns root`() {
		assertEquals("root", NadleKernel.deriveWorkspaceId("."))
	}

	@Test
	fun `deriveWorkspaceId forward slash path`() {
		assertEquals("packages:foo", NadleKernel.deriveWorkspaceId("packages/foo"))
	}

	@Test
	fun `deriveWorkspaceId nested path`() {
		assertEquals(
			"packages:shared:utils",
			NadleKernel.deriveWorkspaceId("packages/shared/utils")
		)
	}

	@Test
	fun `deriveWorkspaceId backslash path`() {
		assertEquals(
			"packages:foo",
			NadleKernel.deriveWorkspaceId("packages\\foo")
		)
	}

	@Test
	fun `deriveWorkspaceId mixed separators`() {
		assertEquals(
			"packages:shared:utils",
			NadleKernel.deriveWorkspaceId("packages\\shared/utils")
		)
	}

	// --- isRootWorkspaceId ---

	@Test
	fun `isRootWorkspaceId returns true for root`() {
		assertTrue(NadleKernel.isRootWorkspaceId("root"))
	}

	@Test
	fun `isRootWorkspaceId returns false for other ids`() {
		assertFalse(NadleKernel.isRootWorkspaceId("packages:foo"))
	}

	@Test
	fun `isRootWorkspaceId returns false for empty string`() {
		assertFalse(NadleKernel.isRootWorkspaceId(""))
	}
}
